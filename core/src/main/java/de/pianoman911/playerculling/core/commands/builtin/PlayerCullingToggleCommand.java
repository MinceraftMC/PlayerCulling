package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.MultiPlayerResolver;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.argument;
import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingToggleCommand {

    private PlayerCullingToggleCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("toggle")
                .requires(ctx -> ctx.getSender().hasPermission("playerculling.command.toggle"))
                .then(literal("global")
                        .requires(ctx -> ctx.getSender().hasPermission("playerculling.command.toggle.global"))
                        .executes(ctx -> executeGlobal(
                                ctx.getSource().getSender(),
                                !ship.isCullingEnabled(), // toggle
                                ship
                        ))
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> executeGlobal(
                                        ctx.getSource().getSender(),
                                        BoolArgumentType.getBool(ctx, "enabled"),
                                        ship
                                ))
                        ))
                .then(literal("player")
                        .requires(ctx -> ctx.getSender().hasPermission("playerculling.command.toggle.player"))
                        .then(argument("target", ship.getPlatform().getArgumentProvider().players())
                                .executes(ctx -> executePlayer(
                                        ctx.getSource().getSender(),
                                        ctx.getArgument("target", MultiPlayerResolver.class).resolve(ctx.getSource()),
                                        TriState.NOT_SET, // toggle
                                        ship
                                ))
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> executePlayer(
                                                ctx.getSource().getSender(),
                                                ctx.getArgument("target", MultiPlayerResolver.class).resolve(ctx.getSource()),
                                                TriState.byBoolean(BoolArgumentType.getBool(ctx, "enabled")),
                                                ship
                                        ))
                                )
                        )
                );

    }

    private static int executeGlobal(PlatformCommandSender sender, boolean enabled, CullShip ship) {
        ship.toggleCulling(enabled);
        sender.sendMessage(text("Culling is now " + (enabled ? "enabled" : "disabled"),
                enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int executePlayer(PlatformCommandSender sender, Collection<PlatformPlayer> target, TriState state, CullShip ship) {
        if (state == TriState.NOT_SET) {
            List<String> enabled = new ArrayList<>();
            List<String> disabled = new ArrayList<>();
            for (PlatformPlayer player : target) {
                CullPlayer cullPlayer = ship.getPlayer(player.getUniqueId());
                if (cullPlayer.isCullingEnabled()) {
                    cullPlayer.setCullingEnabled(false);
                    disabled.add(player.getName());
                } else {
                    cullPlayer.setCullingEnabled(true);
                    enabled.add(player.getName());
                }
            }
            if (!enabled.isEmpty()) {
                sender.sendMessage(text("Culling is now enabled for " + String.join(", ", enabled), NamedTextColor.GREEN));
            }
            if (!disabled.isEmpty()) {
                sender.sendMessage(text("Culling is now disabled for " + String.join(", ", disabled), NamedTextColor.RED));
            }
        } else {
            boolean enabled = Boolean.TRUE.equals(state.toBoolean());
            String names = target.stream().map(PlatformPlayer::getName).collect(Collectors.joining(", "));
            for (PlatformPlayer player : target) {
                CullPlayer cullPlayer = ship.getPlayer(player.getUniqueId());
                cullPlayer.setCullingEnabled(enabled);
            }

            sender.sendMessage(text("Culling is now " + (enabled ? "enabled" : "disabled") + " for " + names,
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }
}