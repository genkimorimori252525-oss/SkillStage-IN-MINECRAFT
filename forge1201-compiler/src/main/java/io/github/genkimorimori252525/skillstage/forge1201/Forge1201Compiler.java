package io.github.genkimorimori252525.skillstage.forge1201;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.genkimorimori252525.skillstage.integration.EditabilityLevel;
import io.github.genkimorimori252525.skillstage.integration.FidelityReport;
import io.github.genkimorimori252525.skillstage.integration.NativeArtifact;
import io.github.genkimorimori252525.skillstage.integration.PortDescriptor;
import io.github.genkimorimori252525.skillstage.integration.PortRole;
import io.github.genkimorimori252525.skillstage.integration.ReplayLevel;
import io.github.genkimorimori252525.skillstage.integration.RepresentationLevel;
import io.github.genkimorimori252525.skillstage.integration.RuntimeTargetPort;
import io.github.genkimorimori252525.skillstage.integration.TargetCompilation;
import io.github.genkimorimori252525.skillstage.integration.TargetSupportLevel;
import io.github.genkimorimori252525.skillstage.integration.VfxCapability;
import io.github.genkimorimori252525.skillstage.integration.VfxPortException;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;
import io.github.genkimorimori252525.skillstage.photon2.Photon2AuthoringPort;
import io.github.genkimorimori252525.skillstage.runtime.ParticleEmitterPlan;
import io.github.genkimorimori252525.skillstage.runtime.VfxRuntimePlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** First fail-closed compiler from the P2 Photon semantic slice to the Forge 1.20.1 handoff. */
public final class Forge1201Compiler implements RuntimeTargetPort {
    private static final PortDescriptor DESCRIPTOR = new PortDescriptor(
            "forge1201-photon1-runtime",
            "Forge 1.20.1 / Photon 1.1.17 Runtime",
            PortRole.RUNTIME_TARGET,
            "Minecraft 1.20.1 / Forge 47.1.3 / Photon 1.1.17",
            Forge1201TargetPackage.FORMAT_ID,
            Set.of(VfxCapability.PARTICLE)
    );

    @Override
    public PortDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public TargetCompilation compile(KvfxProject project) throws VfxPortException {
        var diagnostics = new ArrayList<CompileDiagnostic>();
        var particles = new ArrayList<ParticleEmitterPlan>();
        JsonObject index = readObject(project, Photon2AuthoringPort.INDEX_PATH, diagnostics, "SOURCE_INDEX");
        JsonElement particleElement = readJson(project, Photon2AuthoringPort.PARTICLES_PATH, diagnostics, "PARTICLE_SEMANTICS");

        Set<Integer> expectedParticleIndexes = new HashSet<>();
        if (index != null) inspectIndex(index, expectedParticleIndexes, diagnostics);
        if (particleElement != null) inspectParticles(particleElement, expectedParticleIndexes, particles, diagnostics);

        if (particles.isEmpty() && diagnostics.stream().noneMatch(CompileDiagnostic::isError)) {
            error(diagnostics, "NO_PARTICLES", Photon2AuthoringPort.PARTICLES_PATH,
                    "No semantically resolved particle emitter is available for the first Forge 1.20.1 runtime slice");
        }

        boolean blocked = diagnostics.stream().anyMatch(CompileDiagnostic::isError);
        VfxRuntimePlan runtimePlan = null;
        if (!blocked) {
            particles.sort(Comparator.comparing(ParticleEmitterPlan::id));
            runtimePlan = VfxRuntimePlan.of(project.manifest().projectId().toString(), particles);
            warning(diagnostics, "UNMAPPED_PARTICLE_MODULES", "semantic/photon2/",
                    "Emission, shape, material/renderer, size/color/rotation, physics, UV, trails, sub-emitters, GPU data and other Photon modules are not mapped yet; the runtime adapter must apply the declared photon1-default-v1 profile. This output is EMULATED, not full-fidelity.");
        }

        Forge1201TargetPackage targetPackage = blocked
                ? Forge1201TargetPackage.blocked(project.manifest().projectId().toString(), diagnostics)
                : Forge1201TargetPackage.ready(project.manifest().projectId().toString(), runtimePlan, diagnostics);

        byte[] bytes = Forge1201TargetPackageJson.encode(targetPackage);
        FidelityReport report = toReport(targetPackage);
        return new TargetCompilation(NativeArtifact.of("effect.kvfx-forge1201.json", bytes), report);
    }

    @Override
    public FidelityReport assess(KvfxProject project) {
        try {
            return compile(project).report();
        } catch (VfxPortException e) {
            return new FidelityReport(RepresentationLevel.OPAQUE, EditabilityLevel.NONE,
                    ReplayLevel.SOURCE_REQUIRED, TargetSupportLevel.BLOCKED, List.of(e.getMessage()));
        }
    }

    private static void inspectIndex(JsonObject index, Set<Integer> expectedParticleIndexes,
                                     List<CompileDiagnostic> diagnostics) {
        if (index.has("parseStatus") && "opaque".equals(string(index, "parseStatus"))) {
            error(diagnostics, "SOURCE_OPAQUE", Photon2AuthoringPort.INDEX_PATH,
                    "Photon source structure is opaque and cannot be compiled semantically");
            return;
        }
        Integer projectVersion = integer(index, "projectVersion");
        if (projectVersion == null || projectVersion != 5) {
            error(diagnostics, "UNSUPPORTED_SOURCE_VERSION", Photon2AuthoringPort.INDEX_PATH + "/projectVersion",
                    "P3 accepts only the pinned Photon 2 project version 5 semantic contract");
        }
        Boolean timeline = bool(index, "timelinePresent");
        if (timeline == null) {
            error(diagnostics, "UNRESOLVED_TIMELINE", Photon2AuthoringPort.INDEX_PATH + "/timelinePresent",
                    "timeline presence must be known before compiling for Photon 1");
        } else if (timeline) {
            error(diagnostics, "UNSUPPORTED_TIMELINE", Photon2AuthoringPort.INDEX_PATH + "/timelinePresent",
                    "Photon 1.1.17 has no Photon-2 Timeline equivalent in this target slice");
        }
        if (!index.has("objects") || !index.get("objects").isJsonArray()) {
            error(diagnostics, "INVALID_OBJECT_INDEX", Photon2AuthoringPort.INDEX_PATH + "/objects",
                    "objects must be an array");
            return;
        }
        Set<Integer> seen = new HashSet<>();
        int arrayIndex = 0;
        for (JsonElement element : index.getAsJsonArray("objects")) {
            String path = Photon2AuthoringPort.INDEX_PATH + "/objects/" + arrayIndex;
            if (!element.isJsonObject()) {
                error(diagnostics, "INVALID_OBJECT_INDEX", path, "object index entry must be an object");
                arrayIndex++;
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            Integer objectIndex = integer(object, "index");
            String type = string(object, "type");
            String semanticStatus = string(object, "semanticStatus");
            if (objectIndex == null || !seen.add(objectIndex)) {
                error(diagnostics, "INVALID_OBJECT_INDEX", path + "/index", "object index must be a unique integer");
            }
            if (!"extracted".equals(semanticStatus)) {
                error(diagnostics, "OPAQUE_OBJECT", path,
                        "Whole FX object is not semantically reconstructed and cannot be omitted from target playback");
            } else if (!"particle_emitter".equals(type)) {
                error(diagnostics, "UNSUPPORTED_OBJECT_TYPE", path + "/type",
                        "Only extracted particle_emitter objects are supported by P3");
            } else if (objectIndex != null) {
                expectedParticleIndexes.add(objectIndex);
            }
            arrayIndex++;
        }
    }

    private static void inspectParticles(JsonElement root, Set<Integer> expectedParticleIndexes,
                                         List<ParticleEmitterPlan> plans, List<CompileDiagnostic> diagnostics) {
        if (!root.isJsonArray()) {
            error(diagnostics, "INVALID_PARTICLE_SEMANTICS", Photon2AuthoringPort.PARTICLES_PATH,
                    "particle semantics must be an array");
            return;
        }
        Set<Integer> seen = new HashSet<>();
        int arrayIndex = 0;
        for (JsonElement element : root.getAsJsonArray()) {
            String path = Photon2AuthoringPort.PARTICLES_PATH + "/" + arrayIndex;
            if (!element.isJsonObject()) {
                error(diagnostics, "INVALID_PARTICLE_SEMANTICS", path, "particle semantic entry must be an object");
                arrayIndex++;
                continue;
            }
            JsonObject p = element.getAsJsonObject();
            Integer objectIndex = integerRequired(p, "objectIndex", path, diagnostics);
            String name = stringRequired(p, "name", path, diagnostics);
            Integer objectVersion = integerRequired(p, "objectVersion", path, diagnostics);
            Integer duration = integerRequired(p, "duration", path, diagnostics);
            Boolean looping = booleanRequired(p, "looping", path, diagnostics);
            Integer prewarm = integerRequired(p, "prewarm", path, diagnostics);
            Integer maxParticles = integerRequired(p, "maxParticles", path, diagnostics);
            Boolean parallelUpdate = booleanRequired(p, "parallelUpdate", path, diagnostics);
            Integer startDelay = integerNumberRequired(p, "startDelay", path, diagnostics);
            Integer startLifetime = integerNumberRequired(p, "startLifetime", path, diagnostics);
            Double startSpeed = doubleRequired(p, "startSpeed", path, diagnostics);

            if (objectIndex != null && !seen.add(objectIndex)) {
                error(diagnostics, "DUPLICATE_PARTICLE_SEMANTICS", path + "/objectIndex",
                        "particle objectIndex is duplicated");
            }
            if (objectIndex != null && !expectedParticleIndexes.contains(objectIndex)) {
                error(diagnostics, "ORPHAN_PARTICLE_SEMANTICS", path + "/objectIndex",
                        "particle semantics do not correspond to an extracted particle object");
            }
            if (objectVersion != null && objectVersion != 2) {
                error(diagnostics, "UNSUPPORTED_PARTICLE_VERSION", path + "/objectVersion",
                        "P3 accepts only particle_emitter object version 2");
            }
            if (prewarm != null && prewarm != 0) {
                error(diagnostics, "UNSUPPORTED_PREWARM", path + "/prewarm",
                        "Photon 1.1.17 has no prewarm field; non-zero prewarm cannot be preserved at this gate");
            }

            if (objectIndex != null && name != null && objectVersion != null && duration != null && looping != null
                    && prewarm != null && maxParticles != null && parallelUpdate != null && startDelay != null
                    && startLifetime != null && startSpeed != null) {
                try {
                    plans.add(new ParticleEmitterPlan("particle-" + objectIndex, name, duration, looping, prewarm,
                            startDelay, startLifetime, startSpeed, maxParticles, parallelUpdate));
                } catch (IllegalArgumentException e) {
                    error(diagnostics, "INVALID_PARTICLE_VALUE", path, e.getMessage());
                }
            }
            arrayIndex++;
        }
        for (Integer expected : expectedParticleIndexes) {
            if (!seen.contains(expected)) {
                error(diagnostics, "MISSING_PARTICLE_SEMANTICS", Photon2AuthoringPort.PARTICLES_PATH,
                        "Missing semantic particle entry for object index " + expected);
            }
        }
    }

    private static FidelityReport toReport(Forge1201TargetPackage targetPackage) {
        List<String> limitations = targetPackage.diagnostics().stream().map(d -> d.code() + ": " + d.message()).toList();
        if (targetPackage.status() == Forge1201PackageStatus.BLOCKED) {
            return new FidelityReport(RepresentationLevel.RECONSTRUCTED, EditabilityLevel.PARTIAL,
                    ReplayLevel.SOURCE_REQUIRED, TargetSupportLevel.BLOCKED, limitations);
        }
        return new FidelityReport(RepresentationLevel.RECONSTRUCTED, EditabilityLevel.PARTIAL,
                ReplayLevel.SOURCE_REQUIRED, TargetSupportLevel.EMULATED, limitations);
    }

    private static JsonObject readObject(KvfxProject project, String path, List<CompileDiagnostic> diagnostics, String code) {
        JsonElement element = readJson(project, path, diagnostics, code);
        if (element == null) return null;
        if (!element.isJsonObject()) {
            error(diagnostics, "INVALID_" + code, path, "expected a JSON object");
            return null;
        }
        return element.getAsJsonObject();
    }

    private static JsonElement readJson(KvfxProject project, String path, List<CompileDiagnostic> diagnostics, String code) {
        String json = project.readUtf8(path).orElse(null);
        if (json == null) {
            error(diagnostics, "MISSING_" + code, path, "required KVFX semantic document is missing");
            return null;
        }
        try {
            return JsonParser.parseString(json);
        } catch (RuntimeException e) {
            error(diagnostics, "INVALID_" + code, path, "invalid JSON: " + e.getMessage());
            return null;
        }
    }

    private static Integer integerRequired(JsonObject object, String key, String path, List<CompileDiagnostic> diagnostics) {
        Integer value = integer(object, key);
        if (value == null) error(diagnostics, "UNRESOLVED_FIELD", path + "/" + key, key + " must be a resolved integer");
        return value;
    }

    private static Integer integerNumberRequired(JsonObject object, String key, String path, List<CompileDiagnostic> diagnostics) {
        Double value = number(object, key);
        if (value == null || !Double.isFinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            error(diagnostics, "UNRESOLVED_FIELD", path + "/" + key,
                    key + " must be a resolved integral constant; curves/random functions are not guessed");
            return null;
        }
        return value.intValue();
    }

    private static Double doubleRequired(JsonObject object, String key, String path, List<CompileDiagnostic> diagnostics) {
        Double value = number(object, key);
        if (value == null || !Double.isFinite(value)) {
            error(diagnostics, "UNRESOLVED_FIELD", path + "/" + key,
                    key + " must be a resolved finite constant; curves/random functions are not guessed");
            return null;
        }
        return value;
    }

    private static Boolean booleanRequired(JsonObject object, String key, String path, List<CompileDiagnostic> diagnostics) {
        Boolean value = bool(object, key);
        if (value == null) error(diagnostics, "UNRESOLVED_FIELD", path + "/" + key, key + " must be a resolved boolean");
        return value;
    }

    private static String stringRequired(JsonObject object, String key, String path, List<CompileDiagnostic> diagnostics) {
        String value = string(object, key);
        if (value == null || value.isBlank()) {
            error(diagnostics, "UNRESOLVED_FIELD", path + "/" + key, key + " must be a resolved non-blank string");
            return null;
        }
        return value;
    }

    private static Integer integer(JsonObject object, String key) {
        Double value = number(object, key);
        if (value == null || !Double.isFinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) return null;
        return value.intValue();
    }

    private static Double number(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isNumber()) return null;
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Boolean bool(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isBoolean()) return null;
        return object.get(key).getAsBoolean();
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()
                || !object.getAsJsonPrimitive(key).isString()) return null;
        return object.get(key).getAsString();
    }

    private static void error(List<CompileDiagnostic> diagnostics, String code, String path, String message) {
        diagnostics.add(new CompileDiagnostic(DiagnosticSeverity.ERROR, code, path, message));
    }

    private static void warning(List<CompileDiagnostic> diagnostics, String code, String path, String message) {
        diagnostics.add(new CompileDiagnostic(DiagnosticSeverity.WARNING, code, path, message));
    }
}