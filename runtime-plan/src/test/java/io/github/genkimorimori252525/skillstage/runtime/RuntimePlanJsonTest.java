package io.github.genkimorimori252525.skillstage.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuntimePlanJsonTest {
    @Test
    void deterministicRoundTrip() throws Exception {
        var plan = VfxRuntimePlan.of("effect-001", List.of(
                new ParticleEmitterPlan("particle-0", "Spark", 120, true, 0, 2, 40, 1.5, 500, false)
        ));
        byte[] first = RuntimePlanJson.encode(plan);
        byte[] second = RuntimePlanJson.encode(plan);
        assertArrayEquals(first, second);
        assertEquals(plan, RuntimePlanJson.decode(first));
        assertTrue(new String(first, StandardCharsets.UTF_8).endsWith("\n"));
    }

    @Test
    void rejectsDuplicateEmitterIds() {
        var a = new ParticleEmitterPlan("same", "A", 1, false, 0, 0, 1, 0, 1, false);
        var b = new ParticleEmitterPlan("same", "B", 1, false, 0, 0, 1, 0, 1, false);
        assertThrows(IllegalArgumentException.class, () -> VfxRuntimePlan.of("effect", List.of(a, b)));
    }

    @Test
    void rejectsInvalidEmitterRangesAndNonFiniteSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> new ParticleEmitterPlan("p", "P", 0, false, 0, 0, 1, 1, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ParticleEmitterPlan("p", "P", 1, false, -1, 0, 1, 1, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ParticleEmitterPlan("p", "P", 1, false, 0, 0, 1, Double.NaN, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ParticleEmitterPlan("p", "P", 1, false, 0, 0, 1, 1, 100_001, false));
    }

    @Test
    void rejectsUnknownSchemaOnDecode() {
        String json = """
                {
                  "format": "kneekura-vfx-runtime-plan",
                  "schemaVersion": 2,
                  "effectId": "effect",
                  "particles": []
                }
                """;
        var error = assertThrows(RuntimePlanFormatException.class,
                () -> RuntimePlanJson.decode(json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(error.getMessage().contains("schemaVersion"));
    }
}