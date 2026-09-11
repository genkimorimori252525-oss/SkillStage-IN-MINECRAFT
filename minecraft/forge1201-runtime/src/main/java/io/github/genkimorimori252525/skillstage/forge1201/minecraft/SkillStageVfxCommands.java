package io.github.genkimorimori252525.skillstage.forge1201.minecraft;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidationException;
import io.github.genkimorimori252525.skillstage.forge1201.runtime.RuntimePackageValidator;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

public final class SkillStageVfxCommands {
    private SkillStageVfxCommands() {}

    public static List<LiteralArgumentBuilder<CommandSourceStack>> createClientCommands() {
        return List.of(
                LiteralArgumentBuilder.<CommandSourceStack>literal("skillstage_vfx_golden")
                        .executes(context -> playGolden()),
                LiteralArgumentBuilder.<CommandSourceStack>literal("skillstage_vfx_play")
                        .executes(context -> playConfig()),
                LiteralArgumentBuilder.<CommandSourceStack>literal("skillstage_vfx_validate")
                        .executes(context -> validateConfig())
        );
    }

    private static int playGolden() {
        try {
            var validated = RuntimePackageValidator.validate(GoldenPackage.loadBytes());
            Photon1201PlaybackAdapter.playNearPlayer(validated);
            message("golden package accepted and started: " + validated.effectId());
            return 1;
        } catch (IOException | RuntimePackageValidationException | RuntimeException e) {
            return fail(e);
        }
    }

    private static int playConfig() {
        try {
            var validated = RuntimePackageValidator.validate(RuntimePackageFiles.loadConfigBytes());
            Photon1201PlaybackAdapter.playNearPlayer(validated);
            message("package accepted and started: " + validated.effectId());
            return 1;
        } catch (IOException | RuntimePackageValidationException | RuntimeException e) {
            return fail(e);
        }
    }

    private static int validateConfig() {
        try {
            var validated = RuntimePackageValidator.validate(RuntimePackageFiles.loadConfigBytes());
            message("package valid: " + validated.effectId() + " (" + validated.particles().size() + " particle emitters)");
            return 1;
        } catch (IOException | RuntimePackageValidationException | RuntimeException e) {
            return fail(e);
        }
    }

    private static int fail(Exception error) {
        if (error instanceof RuntimePackageValidationException validation) {
            message("rejected [" + validation.reason() + "]: " + validation.getMessage());
        } else {
            message("failed: " + error.getMessage());
        }
        return 0;
    }

    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("[KNEEKURA VFX] " + text));
        }
    }
}