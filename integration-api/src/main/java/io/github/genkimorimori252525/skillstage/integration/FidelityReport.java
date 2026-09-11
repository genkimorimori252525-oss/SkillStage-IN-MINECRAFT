package io.github.genkimorimori252525.skillstage.integration;

import java.util.List;
import java.util.Objects;

/** Keeps representation, editability, replay, and target support as independent claims. */
public record FidelityReport(
        RepresentationLevel representation,
        EditabilityLevel editability,
        ReplayLevel replay,
        TargetSupportLevel targetSupport,
        List<String> limitations
) {
    public FidelityReport {
        representation = Objects.requireNonNull(representation, "representation");
        editability = Objects.requireNonNull(editability, "editability");
        replay = Objects.requireNonNull(replay, "replay");
        targetSupport = Objects.requireNonNull(targetSupport, "targetSupport");
        limitations = List.copyOf(Objects.requireNonNull(limitations, "limitations"));
    }
}