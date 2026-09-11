package io.github.genkimorimori252525.skillstage.kvfx;

import java.io.IOException;

/** Thrown when a .kvfxproj package violates the versioned container contract. */
public final class KvfxFormatException extends IOException {
    public KvfxFormatException(String message) {
        super(message);
    }

    public KvfxFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}