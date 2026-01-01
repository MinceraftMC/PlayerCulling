package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingHiddenCommand {

    private PlayerCullingHiddenCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("hidden")
                .requires(ctx -> ctx.getExecutor() instanceof PlatformPlayer && ctx.getSender().hasPermission("playerculling.command.hidden"))
                .executes(ctx -> execute(
                        ctx.getSource().getSender(),
                        (PlatformPlayer) ctx.getSource().getExecutor(),
                        ship
                ));
    }

    private static int execute(PlatformCommandSender sender, PlatformPlayer executor, CullShip ship) {
        CullPlayer cullPlayer = ship.getPlayer(executor.getUniqueId());
        if (cullPlayer == null) {
            sender.sendMessage(text(executor == sender ? "You are not culling." : executor.getName() + " is not a culling player.", NamedTextColor.RED));
            return 0;
        }
        IntSet hidden = cullPlayer.getHidden();
        Set<String> names = new HashSet<>(hidden.size());
//        for (UUID uuid : hidden) {
//            PlatformPlayer player = ship.getPlatform().getPlayer(uuid);
//            names.add(player.getName());
//        }

        if (names.isEmpty()) {
            sender.sendMessage(text(executor == sender ? "You see all players." : executor.getName() + " sees all players.", NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(text(executor == sender ? "You are hiding: " : executor.getName() + " is hiding: ")
                .append(text(String.join(", ", names), NamedTextColor.GREEN)));

        return Command.SINGLE_SUCCESS;
    }
}