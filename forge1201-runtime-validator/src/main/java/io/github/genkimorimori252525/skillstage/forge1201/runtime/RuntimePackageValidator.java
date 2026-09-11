package io.github.genkimorimori252525.skillstage.forge1201.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Independent consumer-side validator for P3 target packages.
 *
 * <p>This class intentionally does not depend on forge1201-compiler or runtime-plan. A producer bug
 * therefore cannot make the runtime accept a package merely because both sides share the same DTO or
 * decoder.</p>
 */
public final class RuntimePackageValidator {
    public static final int MAX_PACKAGE_BYTES = 1_048_576;

    public static final String TARGET_FORMAT = "kneekura-forge1201-target";
    public static final int TARGET_SCHEMA = 1;
    public static final String DEFAULT_PROFILE = "photon1-default-v1";
    public static final String MINECRAFT = "1.20.1";
    public static final String LOADER = "forge";
    public static final String FORGE = "47.1.3";
    public static final String RUNTIME_SUBSTRATE = "photon";
    public static final String PHOTON = "1.1.17";
    public static final String PHOTON_COMMIT = "507499fbe5a3fc72bb90993e059848cbb31f13ca";
    public static final String RUNTIME_PLAN_FORMAT = "kneekura-vfx-runtime-plan";
    public static final int RUNTIME_PLAN_SCHEMA = 1;

    private RuntimePackageValidator() {}

    public static ValidatedRuntimePackage validate(byte[] bytes) throws RuntimePackageValidationException {
        if (bytes == null) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, "package bytes must not be null");
        }
        if (bytes.length > MAX_PACKAGE_BYTES) {
            throw reject(RuntimePackageRejection.TOO_LARGE,
                    "target package exceeds " + MAX_PACKAGE_BYTES + " bytes");
        }

        final JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw reject(RuntimePackageRejection.MALFORMED_JSON, "target package must be a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimePackageValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimePackageValidationException(RuntimePackageRejection.MALFORMED_JSON,
                    "target package is not valid JSON", e);
        }

        if (!TARGET_FORMAT.equals(requiredString(root, "format"))) {
            throw reject(RuntimePackageRejection.FORMAT_MISMATCH, "unexpected target package format");
        }
        if (requiredInt(root, "schemaVersion") != TARGET_SCHEMA) {
            throw reject(RuntimePackageRejection.SCHEMA_MISMATCH, "unsupported target package schemaVersion");
        }
        if (!"ready".equals(requiredString(root, "status"))) {
            throw reject(RuntimePackageRejection.STATUS_NOT_READY, "target package status is not ready");
        }

        String sourceProjectId = requiredString(root, "sourceProjectId");
        String defaultProfile = requiredString(root, "defaultProfile");
        if (!DEFAULT_PROFILE.equals(defaultProfile)) {
            throw reject(RuntimePackageRejection.PROFILE_MISMATCH, "unsupported default profile: " + defaultProfile);
        }
        validateTarget(root);
        validateDiagnostics(root);

        JsonObject plan = requiredObject(root, "runtimePlan", RuntimePackageRejection.RUNTIME_PLAN_MISSING);
        if (!RUNTIME_PLAN_FORMAT.equals(requiredString(plan, "format"))) {
            throw reject(RuntimePackageRejection.RUNTIME_PLAN_FORMAT_MISMATCH, "unexpected runtime-plan format");
        }
        if (requiredInt(plan, "schemaVersion") != RUNTIME_PLAN_SCHEMA) {
            throw reject(RuntimePackageRejection.RUNTIME_PLAN_SCHEMA_MISMATCH,
                    "unsupported runtime-plan schemaVersion");
        }
        String effectId = requiredString(plan, "effectId");
        if (!plan.has("particles") || !plan.get("particles").isJsonArray()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, "runtimePlan.particles must be an array");
        }

        var ids = new HashSet<String>();
        var particles = new ArrayList<ValidatedParticlePlan>();
        int index = 0;
        for (JsonElement element : plan.getAsJsonArray("particles")) {
            if (!element.isJsonObject()) {
                throw reject(RuntimePackageRejection.MALFORMED_JSON,
                        "runtimePlan.particles[" + index + "] must be an object");
            }
            JsonObject particle = element.getAsJsonObject();
            String id = requiredString(particle, "id");
            if (!ids.add(id)) {
                throw reject(RuntimePackageRejection.DUPLICATE_PARTICLE_ID, "duplicate particle id: " + id);
            }
            String name = requiredString(particle, "name");
            int duration = requiredInt(particle, "durationTicks");
            boolean looping = requiredBoolean(particle, "looping");
            int prewarm = requiredInt(particle, "prewarmTicks");
            int delay = requiredInt(particle, "startDelayTicks");
            int lifetime = requiredInt(particle, "startLifetimeTicks");
            double speed = requiredDouble(particle, "startSpeed");
            int maxParticles = requiredInt(particle, "maxParticles");
            boolean parallel = requiredBoolean(particle, "parallelUpdate");

            if (duration < 1 || prewarm < 0 || delay < 0 || lifetime < 0
                    || maxParticles < 0 || maxParticles > 100_000) {
                throw reject(RuntimePackageRejection.INVALID_VALUE,
                        "invalid lifecycle/range value in runtimePlan.particles[" + index + "]");
            }
            if (prewarm != 0) {
                throw reject(RuntimePackageRejection.UNSUPPORTED_PREWARM,
                        "Photon 1.1.17 profile requires prewarmTicks = 0");
            }
            particles.add(new ValidatedParticlePlan(id, name, duration, looping, prewarm, delay,
                    lifetime, speed, maxParticles, parallel));
            index++;
        }

        return new ValidatedRuntimePackage(sourceProjectId, effectId, defaultProfile, particles);
    }

    private static void validateTarget(JsonObject root) throws RuntimePackageValidationException {
        JsonObject target = requiredObject(root, "target", RuntimePackageRejection.TARGET_MISMATCH);
        boolean matches = MINECRAFT.equals(requiredString(target, "minecraft"))
                && LOADER.equals(requiredString(target, "loader"))
                && FORGE.equals(requiredString(target, "forge"))
                && RUNTIME_SUBSTRATE.equals(requiredString(target, "runtimeSubstrate"))
                && PHOTON.equals(requiredString(target, "photon"))
                && PHOTON_COMMIT.equals(requiredString(target, "photonCommit"));
        if (!matches) {
            throw reject(RuntimePackageRejection.TARGET_MISMATCH, "target package runtime identity mismatch");
        }
    }

    private static void validateDiagnostics(JsonObject root) throws RuntimePackageValidationException {
        if (!root.has("diagnostics") || !root.get("diagnostics").isJsonArray()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, "diagnostics must be an array");
        }
        int index = 0;
        for (JsonElement element : root.getAsJsonArray("diagnostics")) {
            if (!element.isJsonObject()) {
                throw reject(RuntimePackageRejection.MALFORMED_JSON, "diagnostics[" + index + "] must be an object");
            }
            JsonObject diagnostic = element.getAsJsonObject();
            String severity = requiredString(diagnostic, "severity");
            requiredString(diagnostic, "code");
            requiredString(diagnostic, "path");
            requiredString(diagnostic, "message");
            if ("error".equals(severity)) {
                throw reject(RuntimePackageRejection.ERROR_DIAGNOSTIC,
                        "ready package contains an error diagnostic");
            }
            if (!"warning".equals(severity)) {
                throw reject(RuntimePackageRejection.INVALID_VALUE,
                        "unknown diagnostic severity: " + severity);
            }
            index++;
        }
    }

    private static JsonObject requiredObject(JsonObject object, String key, RuntimePackageRejection reason)
            throws RuntimePackageValidationException {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonObject()) {
            throw reject(reason, key + " must be an object");
        }
        return object.getAsJsonObject(key);
    }

    private static String requiredString(JsonObject object, String key) throws RuntimePackageValidationException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isString()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, key + " must be a string");
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw reject(RuntimePackageRejection.INVALID_VALUE, key + " must not be blank");
        }
        return value;
    }

    private static int requiredInt(JsonObject object, String key) throws RuntimePackageValidationException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isNumber()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, key + " must be a number");
        }
        double value;
        try {
            value = object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            throw new RuntimePackageValidationException(RuntimePackageRejection.INVALID_VALUE,
                    key + " must be an integer", e);
        }
        if (!Double.isFinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw reject(RuntimePackageRejection.INVALID_VALUE, key + " must be an integer");
        }
        return (int) value;
    }

    private static double requiredDouble(JsonObject object, String key) throws RuntimePackageValidationException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isNumber()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, key + " must be a number");
        }
        double value;
        try {
            value = object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            throw new RuntimePackageValidationException(RuntimePackageRejection.INVALID_VALUE,
                    key + " must be finite", e);
        }
        if (!Double.isFinite(value)) {
            throw reject(RuntimePackageRejection.INVALID_VALUE, key + " must be finite");
        }
        return value;
    }

    private static boolean requiredBoolean(JsonObject object, String key) throws RuntimePackageValidationException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isBoolean()) {
            throw reject(RuntimePackageRejection.MALFORMED_JSON, key + " must be a boolean");
        }
        return object.get(key).getAsBoolean();
    }

    private static RuntimePackageValidationException reject(RuntimePackageRejection reason, String message) {
        return new RuntimePackageValidationException(reason, message);
    }
}