package io.github.genkimorimori252525.skillstage.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Versioned, loader-neutral execution contract shared by target adapters. */
public record VfxRuntimePlan(
        int schemaVersion,
        String effectId,
        List<ParticleEmitterPlan> particles
) {
    public static final String FORMAT_ID = "kneekura-vfx-runtime-plan";
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public VfxRuntimePlan {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported runtime-plan schemaVersion: " + schemaVersion);
        }
        if (effectId == null || effectId.isBlank()) throw new IllegalArgumentException("effectId must not be blank");
        particles = List.copyOf(Objects.requireNonNull(particles, "particles"));
        var ids = new HashSet<String>();
        for (ParticleEmitterPlan particle : particles) {
            Objects.requireNonNull(particle, "particle");
            if (!ids.add(particle.id())) throw new IllegalArgumentException("Duplicate particle id: " + particle.id());
        }
    }

    public static VfxRuntimePlan of(String effectId, List<ParticleEmitterPlan> particles) {
        return new VfxRuntimePlan(CURRENT_SCHEMA_VERSION, effectId, particles);
    }
}