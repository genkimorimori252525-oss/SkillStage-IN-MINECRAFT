package io.github.genkimorimori252525.skillstage.integration;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** A provider-owned artifact plus optional sidecars, with defensive byte copies. */
public final class NativeArtifact {
    private final String name;
    private final byte[] primaryBytes;
    private final Map<String, byte[]> sidecars;

    public NativeArtifact(String name, byte[] primaryBytes, Map<String, byte[]> sidecars) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (primaryBytes == null) throw new IllegalArgumentException("primaryBytes must not be null");
        this.name = name;
        this.primaryBytes = primaryBytes.clone();
        TreeMap<String, byte[]> copy = new TreeMap<>();
        if (sidecars != null) {
            sidecars.forEach((path, bytes) -> {
                if (path == null || path.isBlank()) throw new IllegalArgumentException("sidecar path must not be blank");
                if (bytes == null) throw new IllegalArgumentException("sidecar bytes must not be null");
                copy.put(path, bytes.clone());
            });
        }
        this.sidecars = Collections.unmodifiableMap(copy);
    }

    public static NativeArtifact of(String name, byte[] primaryBytes) {
        return new NativeArtifact(name, primaryBytes, Map.of());
    }

    public String name() {
        return name;
    }

    public byte[] primaryBytes() {
        return primaryBytes.clone();
    }

    public Map<String, byte[]> sidecars() {
        TreeMap<String, byte[]> copy = new TreeMap<>();
        sidecars.forEach((path, bytes) -> copy.put(path, bytes.clone()));
        return Collections.unmodifiableMap(copy);
    }
}