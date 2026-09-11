package io.github.genkimorimori252525.skillstage.runtime;

/**
 * Loader-neutral lifecycle/start-state slice for one particle emitter.
 *
 * <p>This deliberately does not pretend that emission, shape, material, renderer, color, size,
 * physics, trails, or sub-emitters have been reconstructed yet. Those arrive as separately proven
 * capabilities instead of being hidden behind defaults in this model.</p>
 */
public record ParticleEmitterPlan(
        String id,
        String name,
        int durationTicks,
        boolean looping,
        int prewarmTicks,
        int startDelayTicks,
        int startLifetimeTicks,
        double startSpeed,
        int maxParticles,
        boolean parallelUpdate
) {
    public ParticleEmitterPlan {
        id = requireText(id, "id");
        name = requireText(name, "name");
        if (durationTicks < 1) throw new IllegalArgumentException("durationTicks must be >= 1");
        if (prewarmTicks < 0) throw new IllegalArgumentException("prewarmTicks must be >= 0");
        if (startDelayTicks < 0) throw new IllegalArgumentException("startDelayTicks must be >= 0");
        if (startLifetimeTicks < 0) throw new IllegalArgumentException("startLifetimeTicks must be >= 0");
        if (!Double.isFinite(startSpeed)) throw new IllegalArgumentException("startSpeed must be finite");
        if (maxParticles < 0 || maxParticles > 100_000) {
            throw new IllegalArgumentException("maxParticles must be in 0..100000");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}