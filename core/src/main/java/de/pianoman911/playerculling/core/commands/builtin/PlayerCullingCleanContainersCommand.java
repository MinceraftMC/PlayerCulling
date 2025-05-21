package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.argument;
import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;


public final class PlayerCullingCleanContainersCommand {

    private PlayerCullingCleanContainersCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("cleancontainers")
                .requires(ctx -> ctx.getSender().hasPermission("playerculling.command.cleancontainers"))
                .executes(ctx -> execute(ctx.getSource().getSender(), ship, true))
                .then(argument("force", BoolArgumentType.bool())
                        .executes(ctx -> execute(
                                ctx.getSource().getSender(),
                                ship,
                                BoolArgumentType.getBool(ctx, "force"))
                        )
                );
    }

    private static int execute(PlatformCommandSender sender, CullShip chip, boolean force) {
        int cleaned = chip.cleanContainers(force);
        sender.sendMessage(text("Cleaned " + cleaned + " containers"));

        return Command.SINGLE_SUCCESS;
    }
}