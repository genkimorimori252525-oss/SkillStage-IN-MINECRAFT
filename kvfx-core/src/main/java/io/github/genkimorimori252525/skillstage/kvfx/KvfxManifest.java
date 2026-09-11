package io.github.genkimorimori252525.skillstage.kvfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed view over manifest.json while preserving every unknown JSON member.
 *
 * <p>The raw document is intentionally retained instead of deserializing into a closed POJO. This
 * lets newer producers add metadata without older readers silently deleting it during a read/write
 * round trip.</p>
 */
public final class KvfxManifest {
    public static final String FORMAT_ID = "kneekura-vfx-project";
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final JsonObject document;

    private KvfxManifest(JsonObject document) {
        this.document = document.deepCopy();
    }

    public static KvfxManifest create(UUID projectId, String creatorName, String creatorVersion) {
        Objects.requireNonNull(projectId, "projectId");
        if (creatorName == null || creatorName.isBlank()) {
            throw new IllegalArgumentException("creatorName must not be blank");
        }
        if (creatorVersion == null || creatorVersion.isBlank()) {
            throw new IllegalArgumentException("creatorVersion must not be blank");
        }

        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT_ID);
        root.addProperty("schemaVersion", CURRENT_SCHEMA_VERSION);
        root.addProperty("projectId", projectId.toString());

        JsonObject createdBy = new JsonObject();
        createdBy.addProperty("name", creatorName);
        createdBy.addProperty("version", creatorVersion);
        root.add("createdBy", createdBy);

        JsonObject sections = new JsonObject();
        sections.addProperty("semantic", "semantic/");
        sections.addProperty("authoring", "authoring/");
        sections.addProperty("opaque", "opaque/");
        sections.addProperty("evidence", "evidence/");
        sections.addProperty("assets", "assets/");
        sections.addProperty("targets", "targets/");
        root.add("sections", sections);
        return new KvfxManifest(root);
    }

    public static KvfxManifest parse(byte[] utf8Json) throws KvfxFormatException {
        Objects.requireNonNull(utf8Json, "utf8Json");
        try {
            JsonElement parsed = JsonParser.parseString(new String(utf8Json, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new KvfxFormatException("manifest.json must contain a JSON object");
            }
            return fromDocument(parsed.getAsJsonObject());
        } catch (JsonParseException e) {
            throw new KvfxFormatException("manifest.json is not valid JSON", e);
        }
    }

    public static KvfxManifest fromDocument(JsonObject source) throws KvfxFormatException {
        Objects.requireNonNull(source, "source");
        JsonObject copy = source.deepCopy();
        validate(copy);
        return new KvfxManifest(copy);
    }

    private static void validate(JsonObject root) throws KvfxFormatException {
        String format = requiredString(root, "format");
        if (!FORMAT_ID.equals(format)) {
            throw new KvfxFormatException("Unsupported KVFX format: " + format);
        }

        if (!root.has("schemaVersion") || !root.get("schemaVersion").isJsonPrimitive()
                || !root.getAsJsonPrimitive("schemaVersion").isNumber()) {
            throw new KvfxFormatException("manifest schemaVersion must be a number");
        }
        int schemaVersion;
        try {
            schemaVersion = root.get("schemaVersion").getAsInt();
        } catch (RuntimeException e) {
            throw new KvfxFormatException("manifest schemaVersion is not an integer", e);
        }
        if (schemaVersion <= 0) {
            throw new KvfxFormatException("manifest schemaVersion must be > 0");
        }
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new KvfxFormatException("KVFX schema " + schemaVersion
                    + " is newer than supported schema " + CURRENT_SCHEMA_VERSION);
        }

        String projectId = requiredString(root, "projectId");
        try {
            UUID.fromString(projectId);
        } catch (IllegalArgumentException e) {
            throw new KvfxFormatException("manifest projectId must be a UUID", e);
        }
    }

    private static String requiredString(JsonObject root, String name) throws KvfxFormatException {
        if (!root.has(name) || !root.get(name).isJsonPrimitive()
                || !root.getAsJsonPrimitive(name).isString()) {
            throw new KvfxFormatException("manifest " + name + " must be a string");
        }
        String value = root.get(name).getAsString();
        if (value.isBlank()) {
            throw new KvfxFormatException("manifest " + name + " must not be blank");
        }
        return value;
    }

    public int schemaVersion() {
        return document.get("schemaVersion").getAsInt();
    }

    public UUID projectId() {
        return UUID.fromString(document.get("projectId").getAsString());
    }

    public JsonObject document() {
        return document.deepCopy();
    }

    public KvfxManifest withExtension(String name, JsonElement value) throws KvfxFormatException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        Objects.requireNonNull(value, "value");
        JsonObject copy = document.deepCopy();
        copy.add(name, value.deepCopy());
        return fromDocument(copy);
    }

    byte[] toUtf8Json() {
        return (GSON.toJson(document) + "\n").getBytes(StandardCharsets.UTF_8);
    }
}