package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidationException;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidator;

import java.io.IOException;

public final class GoldenPackage {
    private static final String RESOURCE = "/assets/skillstage_vfx_runtime/golden/effect.kvfx-forge1201.json";

    private GoldenPackage() {}

    public static byte[] loadBytes() throws IOException, RuntimePackageValidationException {
        try (var input = SkillStageVfxRuntimeMod.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IOException("Embedded golden target package is missing: " + RESOURCE);
            return BoundedPackageReader.read(input, RuntimePackageValidator.MAX_PACKAGE_BYTES);
        }
    }
}