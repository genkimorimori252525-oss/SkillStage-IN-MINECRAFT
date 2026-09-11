package io.github.genkimorimori252525.skillstage.forge1201;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.genkimorimori252525.skillstage.runtime.RuntimePlanFormatException;
import io.github.genkimorimori252525.skillstage.runtime.RuntimePlanJson;
import io.github.genkimorimori252525.skillstage.runtime.VfxRuntimePlan;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class Forge1201TargetPackageJson {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private Forge1201TargetPackageJson() {}

    public static byte[] encode(Forge1201TargetPackage targetPackage) {
        JsonObject root = new JsonObject();
        root.addProperty("format", Forge1201TargetPackage.FORMAT_ID);
        root.addProperty("schemaVersion", targetPackage.schemaVersion());
        root.addProperty("status", targetPackage.status().name().toLowerCase(java.util.Locale.ROOT));
        root.addProperty("sourceProjectId", targetPackage.sourceProjectId());
        root.addProperty("defaultProfile", targetPackage.defaultProfile());

        JsonObject target = new JsonObject();
        target.addProperty("minecraft", Forge1201TargetPackage.MINECRAFT_VERSION);
        target.addProperty("loader", "forge");
        target.addProperty("forge", Forge1201TargetPackage.FORGE_VERSION);
        target.addProperty("runtimeSubstrate", "photon");
        target.addProperty("photon", Forge1201TargetPackage.PHOTON_VERSION);
        target.addProperty("photonCommit", Forge1201TargetPackage.PHOTON_COMMIT);
        root.add("target", target);

        JsonArray diagnostics = new JsonArray();
        for (CompileDiagnostic diagnostic : targetPackage.diagnostics()) {
            JsonObject item = new JsonObject();
            item.addProperty("severity", diagnostic.severity().name().toLowerCase(java.util.Locale.ROOT));
            item.addProperty("code", diagnostic.code());
            item.addProperty("path", diagnostic.path());
            item.addProperty("message", diagnostic.message());
            diagnostics.add(item);
        }
        root.add("diagnostics", diagnostics);

        if (targetPackage.runtimePlan() == null) {
            root.add("runtimePlan", JsonNull.INSTANCE);
        } else {
            root.add("runtimePlan", JsonParser.parseString(
                    new String(RuntimePlanJson.encode(targetPackage.runtimePlan()), StandardCharsets.UTF_8)));
        }
        return (GSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public static Forge1201TargetPackage decode(byte[] utf8Json) throws RuntimePlanFormatException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(utf8Json, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new RuntimePlanFormatException("target package must be a JSON object");
            JsonObject root = parsed.getAsJsonObject();
            if (!Forge1201TargetPackage.FORMAT_ID.equals(requiredString(root, "format"))) {
                throw new RuntimePlanFormatException("unsupported target package format");
            }
            int schema = requiredInt(root, "schemaVersion");
            if (schema != Forge1201TargetPackage.CURRENT_SCHEMA_VERSION) {
                throw new RuntimePlanFormatException("unsupported target package schemaVersion: " + schema);
            }
            String statusText = requiredString(root, "status");
            Forge1201PackageStatus status = switch (statusText) {
                case "ready" -> Forge1201PackageStatus.READY;
                case "blocked" -> Forge1201PackageStatus.BLOCKED;
                default -> throw new RuntimePlanFormatException("unknown target package status: " + statusText);
            };
            String sourceProjectId = requiredString(root, "sourceProjectId");
            String defaultProfile = requiredString(root, "defaultProfile");
            validateTarget(root);

            if (!root.has("diagnostics") || !root.get("diagnostics").isJsonArray()) {
                throw new RuntimePlanFormatException("diagnostics must be an array");
            }
            var diagnostics = new ArrayList<CompileDiagnostic>();
            for (JsonElement element : root.getAsJsonArray("diagnostics")) {
                if (!element.isJsonObject()) throw new RuntimePlanFormatException("diagnostic must be an object");
                JsonObject item = element.getAsJsonObject();
                DiagnosticSeverity severity = switch (requiredString(item, "severity")) {
                    case "error" -> DiagnosticSeverity.ERROR;
                    case "warning" -> DiagnosticSeverity.WARNING;
                    default -> throw new RuntimePlanFormatException("unknown diagnostic severity");
                };
                diagnostics.add(new CompileDiagnostic(severity, requiredString(item, "code"),
                        requiredString(item, "path"), requiredString(item, "message")));
            }

            VfxRuntimePlan plan = null;
            if (root.has("runtimePlan") && !root.get("runtimePlan").isJsonNull()) {
                plan = RuntimePlanJson.decode((GSON.toJson(root.get("runtimePlan")) + "\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            try {
                return new Forge1201TargetPackage(schema, status, sourceProjectId, defaultProfile, plan, diagnostics);
            } catch (IllegalArgumentException e) {
                throw new RuntimePlanFormatException("invalid target package: " + e.getMessage(), e);
            }
        } catch (RuntimePlanFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimePlanFormatException("invalid target package JSON", e);
        }
    }

    private static void validateTarget(JsonObject root) throws RuntimePlanFormatException {
        if (!root.has("target") || !root.get("target").isJsonObject()) {
            throw new RuntimePlanFormatException("target must be an object");
        }
        JsonObject target = root.getAsJsonObject("target");
        if (!Forge1201TargetPackage.MINECRAFT_VERSION.equals(requiredString(target, "minecraft"))
                || !"forge".equals(requiredString(target, "loader"))
                || !Forge1201TargetPackage.FORGE_VERSION.equals(requiredString(target, "forge"))
                || !"photon".equals(requiredString(target, "runtimeSubstrate"))
                || !Forge1201TargetPackage.PHOTON_VERSION.equals(requiredString(target, "photon"))
                || !Forge1201TargetPackage.PHOTON_COMMIT.equals(requiredString(target, "photonCommit"))) {
            throw new RuntimePlanFormatException("target package runtime identity does not match the pinned P3 target");
        }
    }

    private static String requiredString(JsonObject object, String key) throws RuntimePlanFormatException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()) {
            throw new RuntimePlanFormatException(key + " must be a string");
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) throw new RuntimePlanFormatException(key + " must not be blank");
        return value;
    }

    private static int requiredInt(JsonObject object, String key) throws RuntimePlanFormatException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) {
            throw new RuntimePlanFormatException(key + " must be a number");
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value)) throw new RuntimePlanFormatException(key + " must be an integer");
        return (int) value;
    }
}