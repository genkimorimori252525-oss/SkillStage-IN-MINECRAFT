package io.github.genkimorimori252525.skillstage.forge1201.runtime;

/** Stable rejection reasons used by both tests and the later Minecraft command surface. */
public enum RuntimePackageRejection {
    TOO_LARGE,
    MALFORMED_JSON,
    FORMAT_MISMATCH,
    SCHEMA_MISMATCH,
    STATUS_NOT_READY,
    PROFILE_MISMATCH,
    TARGET_MISMATCH,
    ERROR_DIAGNOSTIC,
    RUNTIME_PLAN_MISSING,
    RUNTIME_PLAN_FORMAT_MISMATCH,
    RUNTIME_PLAN_SCHEMA_MISMATCH,
    DUPLICATE_PARTICLE_ID,
    INVALID_VALUE,
    UNSUPPORTED_PREWARM
}