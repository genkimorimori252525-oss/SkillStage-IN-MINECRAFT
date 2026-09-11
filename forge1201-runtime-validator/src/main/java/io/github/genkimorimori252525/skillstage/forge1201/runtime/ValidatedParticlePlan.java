package io.github.genkimorimori252525.skillstage.forge1201.runtime;

/** Runtime-owned particle DTO produced only after independent package validation succeeds. */
public record ValidatedParticlePlan(
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
) {}