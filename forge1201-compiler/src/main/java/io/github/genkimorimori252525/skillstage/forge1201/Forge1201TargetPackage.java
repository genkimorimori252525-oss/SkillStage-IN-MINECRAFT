package io.github.genkimorimori252525.skillstage.forge1201;

import io.github.genkimorimori252525.skillstage.runtime.VfxRuntimePlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Machine-readable handoff from the compiler to the later Forge runtime mod. */
public record Forge1201TargetPackage(
        int schemaVersion,
        Forge1201PackageStatus status,
        String sourceProjectId,
        String defaultProfile,
        VfxRuntimePlan runtimePlan,
        List<CompileDiagnostic> diagnostics
) {
    public static final String FORMAT_ID = "kneekura-forge1201-target";
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String DEFAULT_PROFILE = "photon1-default-v1";
    public static final String MINECRAFT_VERSION = "1.20.1";
    public static final String FORGE_VERSION = "47.1.3";
    public static final String PHOTON_VERSION = "1.1.17";
    public static final String PHOTON_COMMIT = "507499fbe5a3fc72bb90993e059848cbb31f13ca";

    public Forge1201TargetPackage {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("unsupported schemaVersion");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (sourceProjectId == null || sourceProjectId.isBlank()) throw new IllegalArgumentException("sourceProjectId must not be blank");
        if (defaultProfile == null || defaultProfile.isBlank()) throw new IllegalArgumentException("defaultProfile must not be blank");
        var sorted = new ArrayList<>(diagnostics == null ? List.<CompileDiagnostic>of() : diagnostics);
        Collections.sort(sorted);
        diagnostics = List.copyOf(sorted);
        boolean hasError = diagnostics.stream().anyMatch(CompileDiagnostic::isError);
        if (status == Forge1201PackageStatus.READY) {
            if (runtimePlan == null) throw new IllegalArgumentException("READY package requires runtimePlan");
            if (hasError) throw new IllegalArgumentException("READY package must not contain error diagnostics");
        } else {
            if (runtimePlan != null) throw new IllegalArgumentException("BLOCKED package must not contain runtimePlan");
            if (!hasError) throw new IllegalArgumentException("BLOCKED package requires an error diagnostic");
        }
    }

    public static Forge1201TargetPackage ready(String sourceProjectId, VfxRuntimePlan plan,
                                                List<CompileDiagnostic> diagnostics) {
        return new Forge1201TargetPackage(CURRENT_SCHEMA_VERSION, Forge1201PackageStatus.READY,
                sourceProjectId, DEFAULT_PROFILE, plan, diagnostics);
    }

    public static Forge1201TargetPackage blocked(String sourceProjectId, List<CompileDiagnostic> diagnostics) {
        return new Forge1201TargetPackage(CURRENT_SCHEMA_VERSION, Forge1201PackageStatus.BLOCKED,
                sourceProjectId, DEFAULT_PROFILE, null, diagnostics);
    }
}