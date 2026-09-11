package io.github.genkimorimori252525.skillstage.integration;

import java.util.Objects;
import java.util.regex.Pattern;

/** Open capability identifier; unknown future capabilities remain representable. */
public record VfxCapability(String id) implements Comparable<VfxCapability> {
    private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    public static final VfxCapability PARTICLE = new VfxCapability("particle");
    public static final VfxCapability BEAM = new VfxCapability("beam");
    public static final VfxCapability TRAIL = new VfxCapability("trail");
    public static final VfxCapability MESH = new VfxCapability("mesh");
    public static final VfxCapability CURVE = new VfxCapability("curve");
    public static final VfxCapability GRADIENT = new VfxCapability("gradient");
    public static final VfxCapability TIMELINE = new VfxCapability("timeline");
    public static final VfxCapability SHADER = new VfxCapability("shader");
    public static final VfxCapability POST_PROCESS = new VfxCapability("post-process");
    public static final VfxCapability AUDIO = new VfxCapability("audio");
    public static final VfxCapability CAMERA = new VfxCapability("camera");
    public static final VfxCapability FORCE = new VfxCapability("force");
    public static final VfxCapability COLLISION = new VfxCapability("collision");
    public static final VfxCapability SUB_EMITTER = new VfxCapability("sub-emitter");
    public static final VfxCapability CUSTOM_RENDER = new VfxCapability("custom-render");

    public VfxCapability {
        Objects.requireNonNull(id, "id");
        if (!VALID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid capability id: " + id);
        }
    }

    @Override
    public int compareTo(VfxCapability other) {
        return id.compareTo(other.id);
    }
}