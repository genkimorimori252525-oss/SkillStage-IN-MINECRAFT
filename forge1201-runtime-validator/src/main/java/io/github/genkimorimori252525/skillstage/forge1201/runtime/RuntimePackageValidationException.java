package io.github.genkimorimori252525.skillstage.forge1201.runtime;

import java.util.Objects;

public final class RuntimePackageValidationException extends Exception {
    private final RuntimePackageRejection reason;

    public RuntimePackageValidationException(RuntimePackageRejection reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public RuntimePackageValidationException(RuntimePackageRejection reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public RuntimePackageRejection reason() {
        return reason;
    }
}