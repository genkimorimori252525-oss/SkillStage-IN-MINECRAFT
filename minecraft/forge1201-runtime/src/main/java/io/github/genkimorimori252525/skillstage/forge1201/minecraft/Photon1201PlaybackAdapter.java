package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.ValidatedParticlePlan;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.ValidatedRuntimePackage;
import net.minecraft.client.Minecraft;

/** Maps the bounded P4 runtime DTO into the pinned Photon 1.1.17 API. */
public final class Photon1201PlaybackAdapter {
    private Photon1201PlaybackAdapter() {}

    public static FX buildFx(ValidatedRuntimePackage targetPackage) {
        var fx = new FX();
        for (ValidatedParticlePlan plan : targetPackage.particles()) {
            var emitter = new ParticleEmitter();
            emitter.setName(plan.name());
            emitter.config.setDuration(plan.durationTicks());
            emitter.config.setLooping(plan.looping());
            emitter.config.setStartDelay(NumberFunction.constant(plan.startDelayTicks()));
            emitter.config.setStartLifetime(NumberFunction.constant(plan.startLifetimeTicks()));
            emitter.config.setStartSpeed(NumberFunction.constant(plan.startSpeed()));
            emitter.config.setMaxParticles(plan.maxParticles());
            emitter.config.setParallelUpdate(plan.parallelUpdate());

            // P4's explicit approximation profile: everything not reconstructed stays at Photon 1
            // defaults, except emission which is deliberately fixed to one particle per tick.
            emitter.config.emission.setEmissionRate(NumberFunction.constant(1f));
            emitter.config.emission.setEmissionMode(EmissionSetting.Mode.Exacting);
            fx.getMainFX().objects().add(emitter);
        }
        return fx;
    }

    public static void playNearPlayer(ValidatedRuntimePackage targetPackage) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            throw new IllegalStateException("A client world and player are required for VFX playback");
        }
        var origin = minecraft.player.blockPosition().relative(minecraft.player.getDirection(), 2).above();
        var effect = new BlockEffect(buildFx(targetPackage), minecraft.level, origin);
        effect.start();
    }
}