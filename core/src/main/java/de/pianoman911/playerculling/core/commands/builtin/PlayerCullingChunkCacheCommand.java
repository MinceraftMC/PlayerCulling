package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionChunkCache;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.ConcurrentLongCache;
import de.pianoman911.playerculling.platformcommon.util.StringUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingChunkCacheCommand {

    private PlayerCullingChunkCacheCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        return literal("chunkcache")
                .requires(ctx -> ctx.getExecutor() != null && ctx.getSender().hasPermission("playerculling.command.chunkcache"))
                .executes(ctx -> execute(
                        ctx.getSource().getSender(),
                        Objects.requireNonNull(ctx.getSource().getExecutor()),
                        ship
                ));
    }

    private static int execute(PlatformCommandSender sender, PlatformEntity executor, CullShip ship) {
        sender.sendMessage(text("Chunks stored: ", NamedTextColor.GREEN)
                .append(text(executor.getWorld().getOcclusionWorldCache().getChunkCache().size(), NamedTextColor.WHITE)));
        for (PlatformWorld world : ship.getPlatform().getWorlds()) {
            ConcurrentLongCache<OcclusionChunkCache> cache = world.getOcclusionWorldCache().getChunkCache();
            OcclusionChunkCache current = null;
            if (executor.getWorld() == world) {
                int x = executor.getPosition().getFloorX() >> 4;
                int z = executor.getPosition().getFloorZ() >> 4;
                for (OcclusionChunkCache chunk : cache) {
                    if (chunk.getX() == x && chunk.getZ() == z) {
                        current = chunk;
                        break;
                    }
                }
            }

            sender.sendMessage(
                    text("Chunks stored in world ", NamedTextColor.GREEN)
                            .append(text(world.getName(), NamedTextColor.WHITE)).append(text(": ", NamedTextColor.GREEN))
                            .append(text(cache.size(), NamedTextColor.WHITE)).append(text(", current chunk cached: ", NamedTextColor.GREEN))
                            .append(text(current != null, NamedTextColor.WHITE)).append(current == null ? empty() : (
                                    text(" (Height: ", NamedTextColor.GREEN))
                                    .append(text(current.getHeight(), NamedTextColor.WHITE)).append(text(" ; From ", NamedTextColor.GREEN))
                                    .append(text(current.getMinY(), NamedTextColor.WHITE)).append(text("Y to ", NamedTextColor.GREEN))
                                    .append(text(current.getMaxY(), NamedTextColor.WHITE)).append(text("Y) Bytes: ", NamedTextColor.GREEN))
                                    .append(text(StringUtil.toNumInUnits(current.byteSize()), NamedTextColor.WHITE))
                            )

            );
        }
        return Command.SINGLE_SUCCESS;
    }
}
