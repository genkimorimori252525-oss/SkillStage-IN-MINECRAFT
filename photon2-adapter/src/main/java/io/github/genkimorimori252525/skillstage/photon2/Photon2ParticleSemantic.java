package io.github.genkimorimori252525.skillstage.photon2;

public record Photon2ParticleSemantic(
        int objectIndex,
        String name,
        int objectVersion,
        Integer duration,
        Boolean looping,
        Integer prewarm,
        Integer maxParticles,
        Boolean parallelUpdate,
        Double startDelay,
        Double startLifetime,
        Double startSpeed
) {
    public Photon2ParticleSemantic {
        if (objectIndex < 0) throw new IllegalArgumentException("objectIndex must be >= 0");
        if (name == null || name.isBlank()) name = "particle_emitter";
    }
}