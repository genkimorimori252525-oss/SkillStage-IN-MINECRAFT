package io.github.genkimorimori252525.skillstage.kvfx;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class KvfxProjectIOTest {
    @Test
    void roundTripPreservesOpaqueBytesAndUnknownManifestMembers() throws Exception {
        JsonObject document = KvfxManifest.create(
                UUID.fromString("14137da4-5758-4a4d-a472-ab7900d0c09a"),
                "test-suite",
                "1"
        ).document();
        JsonObject futureMetadata = new JsonObject();
        futureMetadata.addProperty("producerNativeVersion", 42);
        futureMetadata.addProperty("note", "must survive old readers");
        document.add("x-future-metadata", futureMetadata);
        KvfxManifest manifest = KvfxManifest.fromDocument(document);

        byte[] opaque = new byte[] {0, 1, 2, 3, (byte) 0xFF, 42};
        KvfxProject project = KvfxProject.builder(manifest)
                .putUtf8("semantic/effect.json", "{\"kind\":\"particle\"}\n")
                .put("authoring/photon2/original.fxproj", new byte[] {9, 8, 7})
                .put("opaque/mod/native.bin", opaque)
                .putUtf8("evidence/runtime/capture.json", "{\"tick\":0}\n")
                .build();

        byte[] archive = KvfxProjectIO.write(project);
        KvfxProject reloaded = KvfxProjectIO.read(archive);

        assertArrayEquals(opaque, reloaded.readEntry("opaque/mod/native.bin").orElseThrow());
        assertArrayEquals(new byte[] {9, 8, 7}, reloaded.readEntry("authoring/photon2/original.fxproj").orElseThrow());
        assertEquals(42, reloaded.manifest().document()
                .getAsJsonObject("x-future-metadata")
                .get("producerNativeVersion")
                .getAsInt());
        assertEquals("must survive old readers", reloaded.manifest().document()
                .getAsJsonObject("x-future-metadata")
                .get("note")
                .getAsString());
    }

    @Test
    void writerIsDeterministicForSameProject() throws Exception {
        KvfxManifest manifest = KvfxManifest.create(
                UUID.fromString("c07ada35-05db-48ca-89e0-56ddceee6e3a"),
                "test-suite",
                "1"
        );
        KvfxProject project = KvfxProject.builder(manifest)
                .putUtf8("semantic/effect.json", "{}\n")
                .put("opaque/a.bin", new byte[] {5, 4, 3, 2, 1})
                .build();

        assertArrayEquals(KvfxProjectIO.write(project), KvfxProjectIO.write(project));
    }

    @Test
    void builderRejectsPathTraversal() {
        KvfxManifest manifest = KvfxManifest.create(UUID.randomUUID(), "test-suite", "1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> KvfxProject.builder(manifest).put("../escape.bin", new byte[] {1}));
        assertTrue(error.getMessage().contains("Unsafe"));
    }

    @Test
    void readerRejectsFutureSchema() throws Exception {
        String manifest = """
                {
                  "format": "kneekura-vfx-project",
                  "schemaVersion": 999,
                  "projectId": "d4915418-7b2a-4e02-811a-3ccca0cf1cc1"
                }
                """;
        byte[] archive = rawArchive("manifest.json", manifest.getBytes(StandardCharsets.UTF_8));

        KvfxFormatException error = assertThrows(KvfxFormatException.class, () -> KvfxProjectIO.read(archive));
        assertTrue(error.getMessage().contains("newer than supported"));
    }

    @Test
    void readerEnforcesPerEntryLimitBeforeMaterializingUnboundedData() throws Exception {
        String manifest = """
                {
                  "format": "kneekura-vfx-project",
                  "schemaVersion": 1,
                  "projectId": "11684834-893a-4f52-a6db-26f105445743"
                }
                """;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry("manifest.json"));
        zip.write(manifest.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.putNextEntry(new ZipEntry("opaque/large.bin"));
        zip.write(new byte[32]);
        zip.closeEntry();
        zip.finish();

        KvfxLimits limits = new KvfxLimits(10, 16, 1024);
        KvfxFormatException error = assertThrows(KvfxFormatException.class,
                () -> KvfxProjectIO.read(new java.io.ByteArrayInputStream(bytes.toByteArray()), limits));
        assertTrue(error.getMessage().contains("maxEntryBytes"));
    }

    private static byte[] rawArchive(String name, byte[] value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value);
        zip.closeEntry();
        zip.finish();
        return bytes.toByteArray();
    }
}