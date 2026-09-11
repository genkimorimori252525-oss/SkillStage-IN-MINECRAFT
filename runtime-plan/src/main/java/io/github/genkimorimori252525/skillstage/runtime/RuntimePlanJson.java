package io.github.genkimorimori252525.skillstage.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Deterministic JSON codec for the loader-neutral runtime plan. */
public final class RuntimePlanJson {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private RuntimePlanJson() {}

    public static byte[] encode(VfxRuntimePlan plan) {
        JsonObject root = new JsonObject();
        root.addProperty("format", VfxRuntimePlan.FORMAT_ID);
        root.addProperty("schemaVersion", plan.schemaVersion());
        root.addProperty("effectId", plan.effectId());
        var particles = new com.google.gson.JsonArray();
        for (ParticleEmitterPlan particle : plan.particles()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", particle.id());
            item.addProperty("name", particle.name());
            item.addProperty("durationTicks", particle.durationTicks());
            item.addProperty("looping", particle.looping());
            item.addProperty("prewarmTicks", particle.prewarmTicks());
            item.addProperty("startDelayTicks", particle.startDelayTicks());
            item.addProperty("startLifetimeTicks", particle.startLifetimeTicks());
            item.addProperty("startSpeed", particle.startSpeed());
            item.addProperty("maxParticles", particle.maxParticles());
            item.addProperty("parallelUpdate", particle.parallelUpdate());
            particles.add(item);
        }
        root.add("particles", particles);
        return (GSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public static VfxRuntimePlan decode(byte[] utf8Json) throws RuntimePlanFormatException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(utf8Json, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new RuntimePlanFormatException("runtime plan must be a JSON object");
            JsonObject root = parsed.getAsJsonObject();
            String format = requiredString(root, "format");
            if (!VfxRuntimePlan.FORMAT_ID.equals(format)) {
                throw new RuntimePlanFormatException("Unsupported runtime-plan format: " + format);
            }
            int schema = requiredInt(root, "schemaVersion");
            if (schema != VfxRuntimePlan.CURRENT_SCHEMA_VERSION) {
                throw new RuntimePlanFormatException("Unsupported runtime-plan schemaVersion: " + schema);
            }
            String effectId = requiredString(root, "effectId");
            if (!root.has("particles") || !root.get("particles").isJsonArray()) {
                throw new RuntimePlanFormatException("particles must be an array");
            }
            var particles = new ArrayList<ParticleEmitterPlan>();
            int index = 0;
            for (JsonElement element : root.getAsJsonArray("particles")) {
                if (!element.isJsonObject()) throw new RuntimePlanFormatException("particles[" + index + "] must be an object");
                JsonObject p = element.getAsJsonObject();
                try {
                    particles.add(new ParticleEmitterPlan(
                            requiredString(p, "id"),
                            requiredString(p, "name"),
                            requiredInt(p, "durationTicks"),
                            requiredBoolean(p, "looping"),
                            requiredInt(p, "prewarmTicks"),
                            requiredInt(p, "startDelayTicks"),
                            requiredInt(p, "startLifetimeTicks"),
                            requiredDouble(p, "startSpeed"),
                            requiredInt(p, "maxParticles"),
                            requiredBoolean(p, "parallelUpdate")
                    ));
                } catch (IllegalArgumentException e) {
                    throw new RuntimePlanFormatException("Invalid particles[" + index + "]: " + e.getMessage(), e);
                }
                index++;
            }
            try {
                return new VfxRuntimePlan(schema, effectId, particles);
            } catch (IllegalArgumentException e) {
                throw new RuntimePlanFormatException("Invalid runtime plan: " + e.getMessage(), e);
            }
        } catch (RuntimePlanFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimePlanFormatException("Invalid runtime-plan JSON", e);
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
        double d = object.get(key).getAsDouble();
        if (!Double.isFinite(d) || d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            throw new RuntimePlanFormatException(key + " must be an integer");
        }
        return (int) d;
    }

    private static double requiredDouble(JsonObject object, String key) throws RuntimePlanFormatException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) {
            throw new RuntimePlanFormatException(key + " must be a number");
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value)) throw new RuntimePlanFormatException(key + " must be finite");
        return value;
    }

    private static boolean requiredBoolean(JsonObject object, String key) throws RuntimePlanFormatException {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isBoolean()) {
            throw new RuntimePlanFormatException(key + " must be a boolean");
        }
        return object.get(key).getAsBoolean();
    }
}