package io.github.genkimorimori252525.skillstage.forge1201;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.genkimorimori252525.skillstage.integration.TargetSupportLevel;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxManifest;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;
import io.github.genkimorimori252525.skillstage.photon2.Photon2AuthoringPort;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Forge1201CompilerTest {
    private static final UUID PROJECT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final Forge1201Compiler compiler = new Forge1201Compiler();

    @Test
    void compilesDeterministicallyToReadyEmulatedPackage() throws Exception {
        KvfxProject project = project(false, false, 0, 500, false);
        var first = compiler.compile(project);
        var second = compiler.compile(project);
        assertArrayEquals(first.artifact().primaryBytes(), second.artifact().primaryBytes());
        assertEquals(TargetSupportLevel.EMULATED, first.report().targetSupport());

        var targetPackage = Forge1201TargetPackageJson.decode(first.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.READY, targetPackage.status());
        assertEquals(PROJECT_ID.toString(), targetPackage.sourceProjectId());
        assertEquals(Forge1201TargetPackage.DEFAULT_PROFILE, targetPackage.defaultProfile());
        assertEquals(1, targetPackage.runtimePlan().particles().size());
        var particle = targetPackage.runtimePlan().particles().get(0);
        assertEquals("Spark", particle.name());
        assertEquals(120, particle.durationTicks());
        assertEquals(2, particle.startDelayTicks());
        assertEquals(40, particle.startLifetimeTicks());
        assertEquals(1.5, particle.startSpeed());
        assertEquals(500, particle.maxParticles());
        assertTrue(targetPackage.diagnostics().stream().anyMatch(d -> d.code().equals("UNMAPPED_PARTICLE_MODULES")));
        assertArrayEquals(first.artifact().primaryBytes(), Forge1201TargetPackageJson.encode(targetPackage));
    }

    @Test
    void nonZeroPrewarmBlocksInsteadOfBeingDropped() throws Exception {
        var result = compiler.compile(project(false, false, 3, 500, false));
        assertEquals(TargetSupportLevel.BLOCKED, result.report().targetSupport());
        var pkg = Forge1201TargetPackageJson.decode(result.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.BLOCKED, pkg.status());
        assertTrue(pkg.diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_PREWARM")));
    }

    @Test
    void timelineBlocksInsteadOfBeingFlattened() throws Exception {
        var result = compiler.compile(project(true, false, 0, 500, false));
        var pkg = Forge1201TargetPackageJson.decode(result.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.BLOCKED, pkg.status());
        assertTrue(pkg.diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_TIMELINE")));
    }

    @Test
    void opaqueWholeObjectBlocksInsteadOfBeingOmitted() throws Exception {
        var result = compiler.compile(project(false, true, 0, 500, false));
        var pkg = Forge1201TargetPackageJson.decode(result.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.BLOCKED, pkg.status());
        assertTrue(pkg.diagnostics().stream().anyMatch(d -> d.code().equals("OPAQUE_OBJECT")));
    }

    @Test
    void unresolvedConstantBlocksInsteadOfGuessing() throws Exception {
        var result = compiler.compile(project(false, false, 0, 500, true));
        var pkg = Forge1201TargetPackageJson.decode(result.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.BLOCKED, pkg.status());
        assertTrue(pkg.diagnostics().stream().anyMatch(d -> d.code().equals("UNRESOLVED_FIELD")
                && d.path().endsWith("/startSpeed")));
    }

    @Test
    void invalidRangeBlocksWithMachineReadableDiagnostic() throws Exception {
        var result = compiler.compile(project(false, false, 0, 100_001, false));
        var pkg = Forge1201TargetPackageJson.decode(result.artifact().primaryBytes());
        assertEquals(Forge1201PackageStatus.BLOCKED, pkg.status());
        assertTrue(pkg.diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARTICLE_VALUE")));
    }

    private static KvfxProject project(boolean timeline, boolean addOpaqueObject, int prewarm,
                                       int maxParticles, boolean unresolvedStartSpeed) throws Exception {
        KvfxManifest manifest = KvfxManifest.create(PROJECT_ID, "test", "1");
        JsonObject index = new JsonObject();
        index.addProperty("sourceKind", "FX_PROJECT");
        index.addProperty("projectVersion", 5);
        index.addProperty("timelinePresent", timeline);
        JsonArray objects = new JsonArray();
        JsonObject particleIndex = new JsonObject();
        particleIndex.addProperty("index", 0);
        particleIndex.addProperty("type", "particle_emitter");
        particleIndex.addProperty("objectVersion", 2);
        particleIndex.addProperty("hasData", true);
        particleIndex.addProperty("semanticStatus", "extracted");
        objects.add(particleIndex);
        if (addOpaqueObject) {
            JsonObject opaque = new JsonObject();
            opaque.addProperty("index", 1);
            opaque.addProperty("type", "future_emitter");
            opaque.addProperty("objectVersion", 1);
            opaque.addProperty("hasData", true);
            opaque.addProperty("semanticStatus", "opaque");
            objects.add(opaque);
        }
        index.add("objects", objects);

        JsonArray particles = new JsonArray();
        JsonObject particle = new JsonObject();
        particle.addProperty("objectIndex", 0);
        particle.addProperty("name", "Spark");
        particle.addProperty("objectVersion", 2);
        particle.addProperty("duration", 120);
        particle.addProperty("looping", true);
        particle.addProperty("prewarm", prewarm);
        particle.addProperty("maxParticles", maxParticles);
        particle.addProperty("parallelUpdate", false);
        particle.addProperty("startDelay", 2.0);
        particle.addProperty("startLifetime", 40.0);
        if (unresolvedStartSpeed) particle.add("startSpeed", JsonNull.INSTANCE);
        else particle.addProperty("startSpeed", 1.5);
        particles.add(particle);

        return KvfxProject.builder(manifest)
                .putUtf8(Photon2AuthoringPort.INDEX_PATH, index.toString())
                .putUtf8(Photon2AuthoringPort.PARTICLES_PATH, particles.toString())
                .build();
    }
}