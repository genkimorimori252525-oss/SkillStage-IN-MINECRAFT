package io.github.genkimorimori252525.skillstage.kvfx;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable in-memory representation of a .kvfxproj package. */
public final class KvfxProject {
    public static final String MANIFEST_PATH = "manifest.json";

    private final KvfxManifest manifest;
    private final NavigableMap<String, byte[]> entries;

    private KvfxProject(KvfxManifest manifest, Map<String, byte[]> entries) {
        this.manifest = manifest;
        TreeMap<String, byte[]> copy = new TreeMap<>();
        entries.forEach((path, bytes) -> {
            String safePath = KvfxPathPolicy.validateFilePath(path);
            if (MANIFEST_PATH.equals(safePath)) {
                throw new IllegalArgumentException("manifest.json is managed by KvfxProject");
            }
            if (bytes == null) throw new IllegalArgumentException("KVFX entry bytes must not be null");
            copy.put(safePath, bytes.clone());
        });
        this.entries = Collections.unmodifiableNavigableMap(copy);
    }

    public KvfxManifest manifest() {
        return manifest;
    }

    public java.util.Set<String> entryPaths() {
        return Collections.unmodifiableSet(entries.navigableKeySet());
    }

    public Optional<byte[]> readEntry(String path) {
        byte[] value = entries.get(path);
        return value == null ? Optional.empty() : Optional.of(value.clone());
    }

    public Optional<String> readUtf8(String path) {
        return readEntry(path).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    Map<String, byte[]> copyEntries() {
        TreeMap<String, byte[]> copy = new TreeMap<>();
        entries.forEach((path, bytes) -> copy.put(path, bytes.clone()));
        return copy;
    }

    public static Builder builder(KvfxManifest manifest) {
        return new Builder(manifest);
    }

    public static final class Builder {
        private final KvfxManifest manifest;
        private final TreeMap<String, byte[]> entries = new TreeMap<>();

        private Builder(KvfxManifest manifest) {
            if (manifest == null) throw new IllegalArgumentException("manifest must not be null");
            this.manifest = manifest;
        }

        public Builder put(String path, byte[] bytes) {
            String safePath = KvfxPathPolicy.validateFilePath(path);
            if (MANIFEST_PATH.equals(safePath)) {
                throw new IllegalArgumentException("manifest.json is managed by KvfxProject");
            }
            if (bytes == null) throw new IllegalArgumentException("bytes must not be null");
            entries.put(safePath, bytes.clone());
            return this;
        }

        public Builder putUtf8(String path, String value) {
            if (value == null) throw new IllegalArgumentException("value must not be null");
            return put(path, value.getBytes(StandardCharsets.UTF_8));
        }

        public KvfxProject build() {
            return new KvfxProject(manifest, entries);
        }
    }
}