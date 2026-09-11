package io.github.genkimorimori252525.skillstage.kvfx;

import java.util.Set;

final class KvfxPathPolicy {
    private static final int MAX_PATH_LENGTH = 1024;
    private static final Set<Character> WINDOWS_FORBIDDEN = Set.of('<', '>', ':', '"', '\\', '|', '?', '*');

    private KvfxPathPolicy() {}

    static String validateFilePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("KVFX entry path must not be blank");
        }
        if (path.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("KVFX entry path is too long");
        }
        if (path.startsWith("/") || path.endsWith("/") || path.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("KVFX entry path must be a normalized relative path: " + path);
        }

        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Unsafe KVFX entry path: " + path);
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (Character.isISOControl(c) || WINDOWS_FORBIDDEN.contains(c)) {
                    throw new IllegalArgumentException("Unsafe character in KVFX entry path: " + path);
                }
            }
        }
        return path;
    }
}