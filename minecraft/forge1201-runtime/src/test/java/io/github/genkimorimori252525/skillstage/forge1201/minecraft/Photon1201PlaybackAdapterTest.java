package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import com.lowdragmc.photon.client.gameobject.emitter.data.number.Constant;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidator;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.ValidatedParticlePlan;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.ValidatedRuntimePackage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class Photon1201PlaybackAdapterTest {
    @Test
    void mapsValidatedParticleLifecycleIntoPhoton1Objects() {
        var particle = new ValidatedParticlePlan(
                "p0", "golden", 40, false, 0, 2, 20, 1.25, 64, false);
        var validated = new ValidatedRuntimePackage(
                "source-project", "golden-effect", RuntimePackageValidator.DEFAULT_PROFILE, List.of(particle));

        var fx = Photon1201PlaybackAdapter.buildFx(validated);

        assertEquals(1, fx.getMainFX().objects().size());
        var emitter = assertInstanceOf(ParticleEmitter.class, fx.getMainFX().objects().get(0));
        assertEquals("golden", emitter.getName());
        assertEquals(40, emitter.config.getDuration());
        assertEquals(false, emitter.config.isLooping());
        assertEquals(2, ((Constant) emitter.config.getStartDelay()).getNumber().intValue());
        assertEquals(20, ((Constant) emitter.config.getStartLifetime()).getNumber().intValue());
        assertEquals(1.25, ((Constant) emitter.config.getStartSpeed()).getNumber().doubleValue());
        assertEquals(64, emitter.config.getMaxParticles());
        assertEquals(false, emitter.config.isParallelUpdate());
        assertEquals(1.0, ((Constant) emitter.config.emission.getEmissionRate()).getNumber().doubleValue());
    }

    @Test
    void embeddedGoldenPackagePassesTheSameIndependentValidator() throws Exception {
        var validated = RuntimePackageValidator.validate(GoldenPackage.loadBytes());

        assertEquals("skillstage-golden", validated.sourceProjectId());
        assertEquals("golden-particle-v1", validated.effectId());
        assertEquals(1, validated.particles().size());
    }

    @Test
    void exposesGoldenPlayAndValidateClientCommandRoots() {
        Set<String> roots = SkillStageVfxCommands.createClientCommands().stream()
                .map(command -> command.getLiteral())
                .collect(Collectors.toSet());

        assertEquals(Set.of("skillstage_vfx_golden", "skillstage_vfx_play", "skillstage_vfx_validate"), roots);
    }
}