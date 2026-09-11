package io.github.genkimorimori252525.skillstage.photon2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.genkimorimori252525.skillstage.integration.AuthoringPort;
import io.github.genkimorimori252525.skillstage.integration.EditabilityLevel;
import io.github.genkimorimori252525.skillstage.integration.FidelityReport;
import io.github.genkimorimori252525.skillstage.integration.NativeArtifact;
import io.github.genkimorimori252525.skillstage.integration.PortDescriptor;
import io.github.genkimorimori252525.skillstage.integration.PortRole;
import io.github.genkimorimori252525.skillstage.integration.ReplayLevel;
import io.github.genkimorimori252525.skillstage.integration.RepresentationLevel;
import io.github.genkimorimori252525.skillstage.integration.TargetSupportLevel;
import io.github.genkimorimori252525.skillstage.integration.VfxCapability;
import io.github.genkimorimori252525.skillstage.integration.VfxPortException;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxFormatException;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxManifest;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Photon2AuthoringPort implements AuthoringPort {
    public static final String NATIVE_PATH = "authoring/photon2/native.bin";
    public static final String SOURCE_PATH = "authoring/photon2/source.json";
    public static final String INDEX_PATH = "semantic/photon2/object-index.json";
    public static final String SIDECAR_MAP_PATH = "authoring/photon2/sidecars.json";
    private static final String SIDECAR_PREFIX = "authoring/photon2/sidecars/";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final PortDescriptor DESCRIPTOR = new PortDescriptor(
            "photon2-authoring",
            "Photon 2 Authoring",
            PortRole.AUTHORING,
            "Minecraft 1.21.1 / Photon 2.2.6.a",
            ".fxproj/.fx",
            Set.of(VfxCapability.PARTICLE, VfxCapability.TIMELINE)
    );

    private final Photon2NativeCodec codec = new Photon2NativeCodec();

    @Override
    public PortDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public KvfxProject importArtifact(NativeArtifact artifact) throws VfxPortException {
        Photon2Document document = codec.parse(artifact);
        byte[] nativeBytes = artifact.primaryBytes();

        JsonObject photonMeta = new JsonObject();
        photonMeta.addProperty("sourceName", artifact.name());
        photonMeta.addProperty("nativeKind", document.kind().name());
        photonMeta.addProperty("projectVersion", document.projectVersion());
        photonMeta.addProperty("sha256", sha256(nativeBytes));
        photonMeta.addProperty("nativeBytes", nativeBytes.length);
        photonMeta.addProperty("objectCount", document.objects().size());
        photonMeta.addProperty("timelinePresent", document.timelinePresent());
        photonMeta.addProperty("upstreamCommit", "609a975cea104fd4e067a94f222a7dd1d619d263");

        KvfxManifest manifest;
        try {
            manifest = KvfxManifest.create(
                    UUID.nameUUIDFromBytes(nativeBytes),
                    "kneekura-photon2-adapter",
                    "0.1.0"
            ).withExtension("photon2", photonMeta);
        } catch (KvfxFormatException e) {
            throw new VfxPortException("Failed to create KVFX manifest", e);
        }

        JsonObject source = photonMeta.deepCopy();
        JsonObject index = new JsonObject();
        index.addProperty("sourceKind", document.kind().name());
        index.addProperty("projectVersion", document.projectVersion());
        index.addProperty("timelinePresent", document.timelinePresent());
        JsonArray objects = new JsonArray();
        for (Photon2ObjectIndex object : document.objects()) {
            JsonObject item = new JsonObject();
            item.addProperty("index", object.index());
            item.addProperty("type", object.type());
            item.addProperty("objectVersion", object.objectVersion());
            item.addProperty("hasData", object.hasData());
            item.addProperty("semanticStatus", object.isParticleEmitter() ? "candidate" : "opaque");
            objects.add(item);
        }
        index.add("objects", objects);

        KvfxProject.Builder builder = KvfxProject.builder(manifest)
                .put(NATIVE_PATH, nativeBytes)
                .putUtf8(SOURCE_PATH, GSON.toJson(source) + "\n")
                .putUtf8(INDEX_PATH, GSON.toJson(index) + "\n");

        JsonObject sidecarMap = new JsonObject();
        for (Map.Entry<String, byte[]> entry : artifact.sidecars().entrySet()) {
            String encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8));
            String storedPath = SIDECAR_PREFIX + encoded + ".bin";
            builder.put(storedPath, entry.getValue());
            sidecarMap.addProperty(entry.getKey(), storedPath);
        }
        builder.putUtf8(SIDECAR_MAP_PATH, GSON.toJson(sidecarMap) + "\n");
        return builder.build();
    }

    @Override
    public NativeArtifact exportProject(KvfxProject project) throws VfxPortException {
        byte[] nativeBytes = project.readEntry(NATIVE_PATH)
                .orElseThrow(() -> new VfxPortException("KVFX project has no preserved Photon 2 native artifact"));

        JsonObject source = parseJsonObject(project, SOURCE_PATH);
        String sourceName = source.has("sourceName") ? source.get("sourceName").getAsString() : "effect.fxproj";

        JsonObject sidecarMap = parseJsonObject(project, SIDECAR_MAP_PATH);
        Map<String, byte[]> sidecars = new LinkedHashMap<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : sidecarMap.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new VfxPortException("Invalid Photon sidecar mapping for " + entry.getKey());
            }
            String storedPath = entry.getValue().getAsString();
            byte[] bytes = project.readEntry(storedPath)
                    .orElseThrow(() -> new VfxPortException("Missing preserved Photon sidecar " + storedPath));
            sidecars.put(entry.getKey(), bytes);
        }
        return new NativeArtifact(sourceName, nativeBytes, sidecars);
    }

    @Override
    public FidelityReport assess(KvfxProject project) {
        boolean nativePresent = project.readEntry(NATIVE_PATH).isPresent();
        if (!nativePresent) {
            return new FidelityReport(
                    RepresentationLevel.OPAQUE,
                    EditabilityLevel.NONE,
                    ReplayLevel.SOURCE_REQUIRED,
                    TargetSupportLevel.BLOCKED,
                    List.of("No preserved Photon 2 native artifact is present")
            );
        }
        List<String> limitations = new ArrayList<>();
        limitations.add("P2 preserves Photon native data exactly but does not yet rewrite semantic edits into Photon NBT");
        limitations.add("Only a bounded ParticleEmitter semantic subset becomes editable in the next capability slice");
        return new FidelityReport(
                RepresentationLevel.NATIVE,
                EditabilityLevel.NONE,
                ReplayLevel.EXACT,
                TargetSupportLevel.SOURCE_REQUIRED,
                limitations
        );
    }

    private static JsonObject parseJsonObject(KvfxProject project, String path) throws VfxPortException {
        String json = project.readUtf8(path)
                .orElseThrow(() -> new VfxPortException("KVFX project is missing " + path));
        try {
            var parsed = com.google.gson.JsonParser.parseString(json);
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("not an object");
            return parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new VfxPortException("Invalid JSON in " + path, e);
        }
    }

    private static String sha256(byte[] bytes) throws VfxPortException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new VfxPortException("SHA-256 unavailable", e);
        }
    }
}