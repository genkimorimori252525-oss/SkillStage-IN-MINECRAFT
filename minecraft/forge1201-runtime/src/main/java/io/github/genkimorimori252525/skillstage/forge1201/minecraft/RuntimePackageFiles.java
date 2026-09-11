package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidationException;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidator;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RuntimePackageFiles {
    static final String DIRECTORY = "skillstage-vfx";
    static final String FILE_NAME = "effect.kvfx-forge1201.json";

    private RuntimePackageFiles() {}

    static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(DIRECTORY).resolve(FILE_NAME);
    }

    static byte[] loadConfigBytes() throws IOException, RuntimePackageValidationException {
        Path path = configPath();
        try (var input = Files.newInputStream(path)) {
            return BoundedPackageReader.read(input, RuntimePackageValidator.MAX_PACKAGE_BYTES);
        }
    }
}