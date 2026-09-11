package io.github.genkimorimori252525.skillstage.photon2;

public enum Photon2NativeKind {
    FX_PROJECT(".fxproj", false),
    FX(".fx", true);

    private final String suffix;
    private final boolean gzip;

    Photon2NativeKind(String suffix, boolean gzip) {
        this.suffix = suffix;
        this.gzip = gzip;
    }

    public String suffix() {
        return suffix;
    }

    public boolean gzip() {
        return gzip;
    }

    public static Photon2NativeKind fromName(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (Photon2NativeKind kind : values()) {
            if (lower.endsWith(kind.suffix)) return kind;
        }
        throw new IllegalArgumentException("Unsupported Photon 2 native artifact: " + name);
    }
}