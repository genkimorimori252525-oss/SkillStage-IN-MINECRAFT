package io.github.genkimorimori252525.skillstage.integration;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Stable description of an external authoring/runtime integration boundary. */
public record PortDescriptor(
        String id,
        String displayName,
        PortRole role,
        String environment,
        String nativeFormat,
        Set<VfxCapability> capabilities
) {
    public PortDescriptor {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        role = Objects.requireNonNull(role, "role");
        environment = requireText(environment, "environment");
        nativeFormat = requireText(nativeFormat, "nativeFormat");
        capabilities = Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(capabilities, "capabilities")));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}