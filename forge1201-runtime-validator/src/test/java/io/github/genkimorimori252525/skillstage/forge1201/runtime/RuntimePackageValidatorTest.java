package io.github.genkimorimori252525.skillstage.forge1201.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimePackageValidatorTest {
    private static final String PARTICLE = """
            {
              "id": "p0",
              "name": "golden",
              "durationTicks": 40,
              "looping": false,
              "prewarmTicks": 0,
              "startDelayTicks": 2,
              "startLifetimeTicks": 20,
              "startSpeed": 1.25,
              "maxParticles": 64,
              "parallelUpdate": false
            }
            """;

    @Test
    void acceptsOnlyThePinnedReadyContract() throws Exception {
        var validated = RuntimePackageValidator.validate(readyJson(PARTICLE, "[]").getBytes(StandardCharsets.UTF_8));

        assertEquals("project-golden", validated.sourceProjectId());
        assertEquals("golden-effect", validated.effectId());
        assertEquals(1, validated.particles().size());
        var particle = validated.particles().get(0);
        assertEquals("p0", particle.id());
        assertEquals(40, particle.durationTicks());
        assertEquals(1.25, particle.startSpeed());
    }

    @Test
    void rejectsBlockedStatusBeforePlayback() {
        String json = readyJson(PARTICLE, "[]").replace("\"status\": \"ready\"", "\"status\": \"blocked\"");
        assertRejected(json, RuntimePackageRejection.STATUS_NOT_READY);
    }

    @Test
    void rejectsMismatchedPinnedRuntimeIdentity() {
        String json = readyJson(PARTICLE, "[]").replace("\"forge\": \"47.1.3\"", "\"forge\": \"47.2.0\"");
        assertRejected(json, RuntimePackageRejection.TARGET_MISMATCH);
    }

    @Test
    void rejectsUnknownDefaultProfile() {
        String json = readyJson(PARTICLE, "[]").replace("photon1-default-v1", "future-profile-v2");
        assertRejected(json, RuntimePackageRejection.PROFILE_MISMATCH);
    }

    @Test
    void rejectsReadyPackageContainingErrorDiagnostic() {
        String diagnostics = """
                [{"severity":"error","code":"FORGED","path":"runtimePlan","message":"must stop"}]
                """;
        assertRejected(readyJson(PARTICLE, diagnostics), RuntimePackageRejection.ERROR_DIAGNOSTIC);
    }

    @Test
    void rejectsDuplicateParticleIdsIndependentlyOfCompiler() {
        assertRejected(readyJson(PARTICLE + "," + PARTICLE, "[]"), RuntimePackageRejection.DUPLICATE_PARTICLE_ID);
    }

    @Test
    void rejectsNonZeroPrewarmBecausePhoton1CannotPreserveIt() {
        String particle = PARTICLE.replace("\"prewarmTicks\": 0", "\"prewarmTicks\": 1");
        assertRejected(readyJson(particle, "[]"), RuntimePackageRejection.UNSUPPORTED_PREWARM);
    }

    @Test
    void rejectsInvalidLifecycleRanges() {
        String particle = PARTICLE.replace("\"durationTicks\": 40", "\"durationTicks\": 0");
        assertRejected(readyJson(particle, "[]"), RuntimePackageRejection.INVALID_VALUE);
    }

    @Test
    void rejectsOversizeInputBeforeJsonParsing() {
        byte[] bytes = new byte[RuntimePackageValidator.MAX_PACKAGE_BYTES + 1];
        var error = assertThrows(RuntimePackageValidationException.class,
                () -> RuntimePackageValidator.validate(bytes));
        assertEquals(RuntimePackageRejection.TOO_LARGE, error.reason());
    }

    private static void assertRejected(String json, RuntimePackageRejection reason) {
        var error = assertThrows(RuntimePackageValidationException.class,
                () -> RuntimePackageValidator.validate(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(reason, error.reason());
    }

    private static String readyJson(String particles, String diagnostics) {
        return """
                {
                  "format": "kneekura-forge1201-target",
                  "schemaVersion": 1,
                  "status": "ready",
                  "sourceProjectId": "project-golden",
                  "defaultProfile": "photon1-default-v1",
                  "target": {
                    "minecraft": "1.20.1",
                    "loader": "forge",
                    "forge": "47.1.3",
                    "runtimeSubstrate": "photon",
                    "photon": "1.1.17",
                    "photonCommit": "507499fbe5a3fc72bb90993e059848cbb31f13ca"
                  },
                  "diagnostics": %s,
                  "runtimePlan": {
                    "format": "kneekura-vfx-runtime-plan",
                    "schemaVersion": 1,
                    "effectId": "golden-effect",
                    "particles": [%s]
                  }
                }
                """.formatted(diagnostics, particles);
    }
}