package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageRejection;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidationException;

import java.io.IOException;
import java.io.InputStream;

final class BoundedPackageReader {
    private BoundedPackageReader() {}

    static byte[] read(InputStream input, int maxBytes) throws IOException, RuntimePackageValidationException {
        byte[] bytes = input.readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) {
            throw new RuntimePackageValidationException(
                    RuntimePackageRejection.TOO_LARGE, "target package exceeds " + maxBytes + " bytes");
        }
        return bytes;
    }
}