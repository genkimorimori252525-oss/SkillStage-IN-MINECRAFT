package io.github.genkimorimori252525.skillstage.forge1201.runtime;

import java.util.List;
import java.util.Objects;

/** Immutable runtime-owned view; producer classes never cross the playback trust boundary. */
public record ValidatedRuntimePackage(
        String sourceProjectId,
        String effectId,
        String defaultProfile,
        List<ValidatedParticlePlan> particles
) {
    public ValidatedRuntimePackage {
        sourceProjectId = Objects.requireNonNull(sourceProjectId, "sourceProjectId");
        effectId = Objects.requireNonNull(effectId, "effectId");
        defaultProfile = Objects.requireNonNull(defaultProfile, "defaultProfile");
        particles = List.copyOf(Objects.requireNonNull(particles, "particles"));
    }
}