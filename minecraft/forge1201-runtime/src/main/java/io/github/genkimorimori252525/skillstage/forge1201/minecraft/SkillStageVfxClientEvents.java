package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = SkillStageVfxRuntimeMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class SkillStageVfxClientEvents {
    private SkillStageVfxClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        SkillStageVfxCommands.createClientCommands().forEach(event.getDispatcher()::register);
    }
}