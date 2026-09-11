package io.github.genkimorimori252525.skillstage.kvfx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Loss-preserving package IO for .kvfxproj ZIP containers. */
public final class KvfxProjectIO {
    private static final int BUFFER_SIZE = 16 * 1024;

    private KvfxProjectIO() {}

    public static KvfxProject read(byte[] archive) throws IOException {
        return read(new ByteArrayInputStream(archive), KvfxLimits.defaults());
    }

    public static KvfxProject read(InputStream input) throws IOException {
        return read(input, KvfxLimits.defaults());
    }

    public static KvfxProject read(InputStream input, KvfxLimits limits) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(limits, "limits");

        ZipInputStream zip = new ZipInputStream(input);
        Set<String> seen = new HashSet<>();
        TreeMap<String, byte[]> entries = new TreeMap<>();
        byte[] manifestBytes = null;
        long totalBytes = 0;
        int fileCount = 0;

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            if (entry.isDirectory()) {
                zip.closeEntry();
                continue;
            }

            String safeName;
            try {
                safeName = KvfxPathPolicy.validateFilePath(name);
            } catch (IllegalArgumentException e) {
                throw new KvfxFormatException("Unsafe entry path: " + name, e);
            }
            if (!seen.add(safeName)) {
                throw new KvfxFormatException("Duplicate KVFX entry: " + safeName);
            }
            fileCount++;
            if (fileCount > limits.maxEntries()) {
                throw new KvfxFormatException("KVFX package exceeds maxEntries=" + limits.maxEntries());
            }

            byte[] bytes = readBounded(zip, limits.maxEntryBytes(), safeName);
            totalBytes += bytes.length;
            if (totalBytes > limits.maxTotalBytes()) {
                throw new KvfxFormatException("KVFX package exceeds maxTotalBytes=" + limits.maxTotalBytes());
            }

            if (KvfxProject.MANIFEST_PATH.equals(safeName)) {
                manifestBytes = bytes;
            } else {
                entries.put(safeName, bytes);
            }
            zip.closeEntry();
        }

        if (manifestBytes == null) {
            throw new KvfxFormatException("KVFX package is missing manifest.json");
        }

        KvfxManifest manifest = KvfxManifest.parse(manifestBytes);
        KvfxProject.Builder builder = KvfxProject.builder(manifest);
        entries.forEach(builder::put);
        return builder.build();
    }

    private static byte[] readBounded(InputStream input, long maxBytes, String name) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long count = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            count += read;
            if (count > maxBytes) {
                throw new KvfxFormatException("KVFX entry exceeds maxEntryBytes: " + name);
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    public static byte[] write(KvfxProject project) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(project, out);
        return out.toByteArray();
    }

    public static void write(KvfxProject project, OutputStream output) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(output, "output");

        ZipOutputStream zip = new ZipOutputStream(output);
        zip.setLevel(Deflater.BEST_COMPRESSION);

        TreeMap<String, byte[]> ordered = new TreeMap<>();
        ordered.put(KvfxProject.MANIFEST_PATH, project.manifest().toUtf8Json());
        for (Map.Entry<String, byte[]> entry : project.copyEntries().entrySet()) {
            ordered.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, byte[]> entry : ordered.entrySet()) {
            ZipEntry zipEntry = new ZipEntry(entry.getKey());
            zipEntry.setTime(0L);
            zip.putNextEntry(zipEntry);
            zip.write(entry.getValue());
            zip.closeEntry();
        }
        zip.finish();
        zip.flush();
    }
}