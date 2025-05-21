package de.pianoman911.playerculling.core.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingBlockDebugCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingChunkCacheCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingChunkSizesCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingCleanContainersCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingContainerViewCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingHiddenCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingPerformanceCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingRayCastDebugCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingReloadConfigCommand;
import de.pianoman911.playerculling.core.commands.builtin.PlayerCullingToggleCommand;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;

import java.util.function.Function;

public final class PlayerCullingCommand {

    private PlayerCullingCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> literal(final String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<PlatformCommandSourceStack, T> argument(final String name, final ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(name, argumentType);
    }

    public static LiteralCommandNode<PlatformCommandSourceStack> create(CullShip ship) {
        return literal("playerculling")
                .requires(css -> css.getSender().hasPermission("playerculling.command"))
                .then(PlayerCullingBlockDebugCommand.getNode(ship))
                .then(PlayerCullingChunkCacheCommand.getNode(ship))
                .then(PlayerCullingChunkSizesCommand.getNode())
                .then(PlayerCullingCleanContainersCommand.getNode(ship))
                .then(PlayerCullingContainerViewCommand.getNode(ship))
                .then(PlayerCullingHiddenCommand.getNode(ship))
                .then(PlayerCullingPerformanceCommand.getNode(ship))
                .then(PlayerCullingRayCastDebugCommand.getNode(ship))
                .then(PlayerCullingReloadConfigCommand.getNode(ship))
                .then(PlayerCullingToggleCommand.getNode(ship))
                .build();
    }

    public static <T> LiteralCommandNode<T> createConverted(
            CullShip ship,
            Function<PlatformCommandSourceStack, T> converter,
            Function<T, PlatformCommandSourceStack> reverseConverter
    ) {
        CommandConversionHandler<T> conversionHandler = new CommandConversionHandler<>(
                converter,
                reverseConverter,
                arg -> ship.getPlatform().getArgumentProvider().mapFromNms(arg),
                arg -> ship.getPlatform().getArgumentProvider().mapToNms(arg)
        );
        return conversionHandler.remapCommand(create(ship));
    }
}
