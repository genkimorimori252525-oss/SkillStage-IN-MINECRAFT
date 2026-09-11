package io.github.genkimorimori252525.skillstage.kvfx;

/** Defensive read limits for untrusted or malformed project packages. */
public record KvfxLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes) {
    public KvfxLimits {
        if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be > 0");
        if (maxEntryBytes <= 0) throw new IllegalArgumentException("maxEntryBytes must be > 0");
        if (maxTotalBytes <= 0) throw new IllegalArgumentException("maxTotalBytes must be > 0");
        if (maxEntryBytes > maxTotalBytes) {
            throw new IllegalArgumentException("maxEntryBytes must be <= maxTotalBytes");
        }
    }

    public static KvfxLimits defaults() {
        return new KvfxLimits(16_384, 64L * 1024 * 1024, 512L * 1024 * 1024);
    }
}