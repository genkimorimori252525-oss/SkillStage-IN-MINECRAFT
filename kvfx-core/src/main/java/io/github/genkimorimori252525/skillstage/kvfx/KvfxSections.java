package io.github.genkimorimori252525.skillstage.kvfx;

/** Canonical top-level sections inside a .kvfxproj package. */
public final class KvfxSections {
    public static final String SEMANTIC = "semantic/";
    public static final String AUTHORING = "authoring/";
    public static final String OPAQUE = "opaque/";
    public static final String EVIDENCE = "evidence/";
    public static final String ASSETS = "assets/";
    public static final String TARGETS = "targets/";

    private KvfxSections() {}

    public static String path(String section, String relativePath) {
        if (section == null || !section.endsWith("/")) {
            throw new IllegalArgumentException("section must be a canonical directory prefix");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        return KvfxPathPolicy.validateFilePath(section + relativePath);
    }
}