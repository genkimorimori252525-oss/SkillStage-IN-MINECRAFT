package io.github.genkimorimori252525.skillstage.integration;

import java.util.Objects;

public record TargetCompilation(NativeArtifact artifact, FidelityReport report) {
    public TargetCompilation {
        artifact = Objects.requireNonNull(artifact, "artifact");
        report = Objects.requireNonNull(report, "report");
    }
}