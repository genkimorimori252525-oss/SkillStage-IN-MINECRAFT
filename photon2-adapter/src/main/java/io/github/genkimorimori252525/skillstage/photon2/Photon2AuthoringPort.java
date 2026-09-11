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
    public static final String PARTICLES_PATH = "semantic/photon2/particles.json";
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
    public PortDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public KvfxProject importArtifact(NativeArtifact artifact) throws VfxPortException {
        byte[] nativeBytes = artifact.primaryBytes();
        codec.validateNativeEnvelope(artifact.name(), nativeBytes);
        Photon2NativeKind nativeKind = codec.nativeKind(artifact.name());

        Photon2Document document = null;
        String parseError = null;
        try {
            document = codec.parse(artifact);
        } catch (VfxPortException e) {
            parseError = e.getMessage();
        }

        JsonObject photonMeta = new JsonObject();
        photonMeta.addProperty("sourceName", artifact.name());
        photonMeta.addProperty("nativeKind", nativeKind.name());
        photonMeta.addProperty("sha256", sha256(nativeBytes));
        photonMeta.addProperty("nativeBytes", nativeBytes.length);
        photonMeta.addProperty("upstreamCommit", "609a975cea104fd4e067a94f222a7dd1d619d263");
        if (document != null) {
            photonMeta.addProperty("parseStatus", "parsed");
            photonMeta.addProperty("projectVersion", document.projectVersion());
            photonMeta.addProperty("objectCount", document.objects().size());
            photonMeta.addProperty("timelinePresent", document.timelinePresent());
            photonMeta.addProperty("semanticParticleCount", document.particles().size());
        } else {
            photonMeta.addProperty("parseStatus", "opaque");
            photonMeta.addProperty("parseError", parseError == null ? "unknown parse failure" : parseError);
        }

        KvfxManifest manifest;
        try {
            manifest = KvfxManifest.create(UUID.nameUUIDFromBytes(nativeBytes), "kneekura-photon2-adapter", "0.2.0")
                    .withExtension("photon2", photonMeta);
        } catch (KvfxFormatException e) {
            throw new VfxPortException("Failed to create KVFX manifest", e);
        }

        KvfxProject.Builder builder = KvfxProject.builder(manifest)
                .put(NATIVE_PATH, nativeBytes)
                .putUtf8(SOURCE_PATH, GSON.toJson(photonMeta) + "\n");

        JsonObject index = new JsonObject();
        JsonArray particles = new JsonArray();
        if (document != null) {
            index.addProperty("sourceKind", document.kind().name());
            index.addProperty("projectVersion", document.projectVersion());
            index.addProperty("timelinePresent", document.timelinePresent());
            JsonArray objects = new JsonArray();
            java.util.Set<Integer> extracted = new java.util.HashSet<>();
            for (Photon2ParticleSemantic particle : document.particles()) extracted.add(particle.objectIndex());
            for (Photon2ObjectIndex object : document.objects()) {
                JsonObject item = new JsonObject();
                item.addProperty("index", object.index());
                item.addProperty("type", object.type());
                item.addProperty("objectVersion", object.objectVersion());
                item.addProperty("hasData", object.hasData());
                item.addProperty("semanticStatus", extracted.contains(object.index()) ? "extracted" : "opaque");
                objects.add(item);
            }
            index.add("objects", objects);

            for (Photon2ParticleSemantic particle : document.particles()) {
                JsonObject item = new JsonObject();
                item.addProperty("objectIndex", particle.objectIndex());
                item.addProperty("name", particle.name());
                item.addProperty("objectVersion", particle.objectVersion());
                addNullable(item, "duration", particle.duration());
                addNullable(item, "looping", particle.looping());
                addNullable(item, "prewarm", particle.prewarm());
                addNullable(item, "maxParticles", particle.maxParticles());
                addNullable(item, "parallelUpdate", particle.parallelUpdate());
                addNullable(item, "startDelay", particle.startDelay());
                addNullable(item, "startLifetime", particle.startLifetime());
                addNullable(item, "startSpeed", particle.startSpeed());
                particles.add(item);
            }
        } else {
            index.addProperty("parseStatus", "opaque");
            index.addProperty("parseError", parseError);
        }
        builder.putUtf8(INDEX_PATH, GSON.toJson(index) + "\n");
        builder.putUtf8(PARTICLES_PATH, GSON.toJson(particles) + "\n");

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
        if (project.readEntry(NATIVE_PATH).isEmpty()) {
            return new FidelityReport(RepresentationLevel.OPAQUE, EditabilityLevel.NONE,
                    ReplayLevel.SOURCE_REQUIRED, TargetSupportLevel.BLOCKED,
                    List.of("No preserved Photon 2 native artifact is present"));
        }
        JsonObject source;
        try {
            source = parseJsonObject(project, SOURCE_PATH);
        } catch (VfxPortException e) {
            return new FidelityReport(RepresentationLevel.NATIVE, EditabilityLevel.NONE,
                    ReplayLevel.EXACT, TargetSupportLevel.SOURCE_REQUIRED,
                    List.of("Native bytes are preserved but Photon source metadata is invalid"));
        }
        List<String> limitations = new ArrayList<>();
        if ("opaque".equals(source.has("parseStatus") ? source.get("parseStatus").getAsString() : "")) {
            limitations.add("Photon structure could not be parsed; the artifact is preserved as opaque native data");
        } else {
            limitations.add("Only Photon project v5 / particle_emitter v2 bounded scalar semantics are extracted");
        }
        limitations.add("P2 does not yet rewrite semantic edits into Photon NBT");
        return new FidelityReport(RepresentationLevel.NATIVE, EditabilityLevel.NONE,
                ReplayLevel.EXACT, TargetSupportLevel.SOURCE_REQUIRED, limitations);
    }

    private static void addNullable(JsonObject object, String key, Number value) {
        if (value == null) object.add(key, com.google.gson.JsonNull.INSTANCE); else object.addProperty(key, value);
    }
    private static void addNullable(JsonObject object, String key, Boolean value) {
        if (value == null) object.add(key, com.google.gson.JsonNull.INSTANCE); else object.addProperty(key, value);
    }

    private static JsonObject parseJsonObject(KvfxProject project, String path) throws VfxPortException {
        String json = project.readUtf8(path).orElseThrow(() -> new VfxPortException("KVFX project is missing " + path));
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