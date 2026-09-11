package io.github.genkimorimori252525.skillstage.photon2;

import com.google.gson.JsonParser;
import io.github.genkimorimori252525.skillstage.integration.NativeArtifact;
import io.github.genkimorimori252525.skillstage.integration.ReplayLevel;
import io.github.genkimorimori252525.skillstage.integration.RepresentationLevel;
import io.github.genkimorimori252525.skillstage.photon2.internal.Nbt;
import io.github.genkimorimori252525.skillstage.photon2.internal.NbtBinary;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Photon2AuthoringPortTest {
    private final Photon2AuthoringPort port = new Photon2AuthoringPort();

    @Test
    void importsExtractsAndExportsFxprojWithoutLosingNativeOrOpaqueData() throws Exception {
        byte[] fixture = NbtBinary.write(fxProjectFixture(5, 2), false);
        byte[] sidecar = new byte[]{9, 8, 7, 6};
        NativeArtifact nativeArtifact = new NativeArtifact("golden.fxproj", fixture, Map.of("textures/glow.png", sidecar));

        var project = port.importArtifact(nativeArtifact);
        assertArrayEquals(fixture, project.readEntry(Photon2AuthoringPort.NATIVE_PATH).orElseThrow());

        var index = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.INDEX_PATH).orElseThrow()).getAsJsonObject();
        var objects = index.getAsJsonArray("objects");
        assertEquals(2, objects.size());
        assertEquals("extracted", objects.get(0).getAsJsonObject().get("semanticStatus").getAsString());
        assertEquals("future_emitter", objects.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("opaque", objects.get(1).getAsJsonObject().get("semanticStatus").getAsString());

        var particles = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.PARTICLES_PATH).orElseThrow()).getAsJsonArray();
        assertEquals(1, particles.size());
        var particle = particles.get(0).getAsJsonObject();
        assertEquals("Spark", particle.get("name").getAsString());
        assertEquals(120, particle.get("duration").getAsInt());
        assertTrue(particle.get("looping").getAsBoolean());
        assertEquals(500, particle.get("maxParticles").getAsInt());
        assertEquals(2.0, particle.get("startDelay").getAsDouble());
        assertEquals(40.0, particle.get("startLifetime").getAsDouble());
        assertEquals(1.5, particle.get("startSpeed").getAsDouble());

        NativeArtifact exported = port.exportProject(project);
        assertEquals("golden.fxproj", exported.name());
        assertArrayEquals(fixture, exported.primaryBytes());
        assertArrayEquals(sidecar, exported.sidecars().get("textures/glow.png"));
        assertEquals(RepresentationLevel.NATIVE, port.assess(project).representation());
        assertEquals(ReplayLevel.EXACT, port.assess(project).replay());
    }

    @Test
    void compressedFxRoundTripsExactly() throws Exception {
        byte[] fixture = NbtBinary.write(fxFixture(), true);
        NativeArtifact artifact = NativeArtifact.of("spell.fx", fixture);
        var project = port.importArtifact(artifact);
        assertArrayEquals(fixture, port.exportProject(project).primaryBytes());
        var source = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.SOURCE_PATH).orElseThrow()).getAsJsonObject();
        assertEquals("FX", source.get("nativeKind").getAsString());
        assertEquals("parsed", source.get("parseStatus").getAsString());
    }

    @Test
    void newerObjectVersionRemainsOpaqueInsteadOfBeingMisinterpreted() throws Exception {
        byte[] fixture = NbtBinary.write(fxProjectFixture(5, 99), false);
        var project = port.importArtifact(NativeArtifact.of("future.fxproj", fixture));
        var particles = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.PARTICLES_PATH).orElseThrow()).getAsJsonArray();
        assertTrue(particles.isEmpty());
        var objects = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.INDEX_PATH).orElseThrow())
                .getAsJsonObject().getAsJsonArray("objects");
        assertEquals("opaque", objects.get(0).getAsJsonObject().get("semanticStatus").getAsString());
        assertArrayEquals(fixture, port.exportProject(project).primaryBytes());
    }

    @Test
    void malformedPhotonNbtIsStillPreservedAsOpaqueNativeData() throws Exception {
        byte[] broken = new byte[]{10, 0, 0, 3, 0};
        var project = port.importArtifact(NativeArtifact.of("broken.fxproj", broken));
        assertArrayEquals(broken, project.readEntry(Photon2AuthoringPort.NATIVE_PATH).orElseThrow());
        assertArrayEquals(broken, port.exportProject(project).primaryBytes());
        var source = JsonParser.parseString(project.readUtf8(Photon2AuthoringPort.SOURCE_PATH).orElseThrow()).getAsJsonObject();
        assertEquals("opaque", source.get("parseStatus").getAsString());
    }

    @Test
    void unsupportedSuffixFailsBeforeCreatingAProject() {
        var error = assertThrows(Exception.class,
                () -> port.importArtifact(NativeArtifact.of("not-photon.bin", new byte[]{1, 2, 3})));
        assertTrue(error.getMessage().contains("Unsupported Photon 2 native artifact"));
    }

    private static Nbt.Document fxProjectFixture(int projectVersion, int particleVersion) {
        var particleData = compound(
                "version", new Nbt.IntTag(particleVersion),
                "name", new Nbt.StringTag("Spark"),
                "config", particleConfig()
        );
        var particleWrapper = compound(
                "type", new Nbt.StringTag("particle_emitter"),
                "data", particleData
        );
        var futureWrapper = compound(
                "type", new Nbt.StringTag("future_emitter"),
                "data", compound("version", new Nbt.IntTag(99), "mystery", new Nbt.StringTag("KEEP_ME"))
        );
        var fxData = compound(
                "fxObjects", new Nbt.ListTag(10, List.of(particleWrapper, futureWrapper)),
                "timeline", compound(
                        "tracks", new Nbt.ListTag(10, List.of()),
                        "markers", new Nbt.ListTag(10, List.of())
                )
        );
        return new Nbt.Document("", compound(
                "meta", compound("version_num", new Nbt.IntTag(projectVersion), "version", new Nbt.StringTag(projectVersion + ".0")),
                "data", compound("fx", compound("fxData", fxData))
        ));
    }

    private static Nbt.Document fxFixture() {
        var fxData = compound("fxObjects", new Nbt.ListTag(10, List.of()));
        return new Nbt.Document("", compound("version", new Nbt.IntTag(5), "fxData", fxData));
    }

    private static Nbt.CompoundTag particleConfig() {
        return compound(
                "duration", new Nbt.IntTag(120),
                "looping", new Nbt.ByteTag((byte) 1),
                "prewarm", new Nbt.IntTag(3),
                "maxParticles", new Nbt.IntTag(500),
                "parallelUpdate", new Nbt.ByteTag((byte) 0),
                "startDelay", constant(new Nbt.IntTag(2)),
                "startLifetime", constant(new Nbt.IntTag(40)),
                "startSpeed", constant(new Nbt.FloatTag(1.5f)),
                "futureModule", compound("value", new Nbt.StringTag("must survive in native bytes"))
        );
    }

    private static Nbt.CompoundTag constant(Nbt.Tag value) {
        return compound("type", new Nbt.StringTag("constant"), "data", compound("number", value));
    }

    private static Nbt.CompoundTag compound(Object... entries) {
        LinkedHashMap<String, Nbt.Tag> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], (Nbt.Tag) entries[i + 1]);
        return new Nbt.CompoundTag(map);
    }
}