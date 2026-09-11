package io.github.genkimorimori252525.skillstage.photon2;

public record Photon2ObjectIndex(
        int index,
        String type,
        int objectVersion,
        boolean hasData
) {
    public Photon2ObjectIndex {
        if (index < 0) throw new IllegalArgumentException("index must be >= 0");
        if (type == null || type.isBlank()) type = "<unknown>";
    }

    public boolean isParticleEmitter() {
        return "particle_emitter".equals(type);
    }
}