package io.github.genkimorimori252525.skillstage.photon2;

import java.util.List;

public final class Photon2Document {
    private final Photon2NativeKind kind;
    private final int projectVersion;
    private final boolean timelinePresent;
    private final List<Photon2ObjectIndex> objects;
    private final byte[] originalBytes;

    Photon2Document(Photon2NativeKind kind, int projectVersion, boolean timelinePresent,
                    List<Photon2ObjectIndex> objects, byte[] originalBytes) {
        this.kind = kind;
        this.projectVersion = projectVersion;
        this.timelinePresent = timelinePresent;
        this.objects = List.copyOf(objects);
        this.originalBytes = originalBytes.clone();
    }

    public Photon2NativeKind kind() {
        return kind;
    }

    public int projectVersion() {
        return projectVersion;
    }

    public boolean timelinePresent() {
        return timelinePresent;
    }

    public List<Photon2ObjectIndex> objects() {
        return objects;
    }

    public byte[] originalBytes() {
        return originalBytes.clone();
    }
}