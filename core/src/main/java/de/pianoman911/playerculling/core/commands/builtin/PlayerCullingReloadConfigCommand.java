package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import net.kyori.adventure.text.format.NamedTextColor;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingReloadConfigCommand {

    private PlayerCullingReloadConfigCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("reloadconfig")
                .requires(ctx -> ctx.getSender().hasPermission("playerculling.command.reloadconfig"))
                .executes(ctx -> execute(ctx.getSource().getSender(), ship));
    }

    private static int execute(PlatformCommandSender sender, CullShip ship) {
        ship.getConfig().reloadConfig();
        sender.sendMessage(text("Config reloaded", NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }
}