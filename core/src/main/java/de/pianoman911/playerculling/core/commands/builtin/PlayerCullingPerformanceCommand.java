package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public final class PlayerCullingPerformanceCommand {

    private PlayerCullingPerformanceCommand() {
    }

    private static Component generatePerformanceInfo(CullShip ship, double ms) {
        return text("Performance: ", NamedTextColor.GREEN)
                .append(text(String.format("%.2f", ms), NamedTextColor.WHITE))
                .append(text("ms - ", NamedTextColor.WHITE))
                .append(text(ship.getPlayers().size(), NamedTextColor.WHITE))
                .append(text(" players - ", NamedTextColor.WHITE))
                .append(text(ship.debugContainersFormat(), NamedTextColor.WHITE));
    }

    private static Component generateChunkCacheInfo(CullShip ship) {
        return text("Occlusion Cache: ", NamedTextColor.GREEN)
                .append(text(OcclusionWorldCache.CACHE_EXECUTOR.getActiveCount(), NamedTextColor.WHITE))
                .append(text('/', NamedTextColor.WHITE))
                .append(text(OcclusionWorldCache.CACHE_EXECUTOR.getLargestPoolSize(), NamedTextColor.WHITE))
                .append(text(" Threads - C/S: ", NamedTextColor.WHITE))
                .append(text(OcclusionWorldCache.CACHE_EXECUTOR.getCompletedTaskCount(), NamedTextColor.WHITE))
                .append(text('/', NamedTextColor.WHITE))
                .append(text(OcclusionWorldCache.chunksStored(ship.getPlatform().getWorlds()), NamedTextColor.WHITE))
                .append(text(" chunks (", NamedTextColor.WHITE))
                .append(text(OcclusionWorldCache.formattedByteSize(ship.getPlatform().getWorlds()), NamedTextColor.WHITE))
                .append(text(")", NamedTextColor.WHITE));
    }

    private static Component generateCulledPlayerInfo(float culledPerPlayer, int culledPlayers) {
        return text("Culled Players: ", NamedTextColor.GREEN)
                .append(text(String.format("%.2f", (culledPerPlayer * 100)), NamedTextColor.WHITE))
                .append(text("% - ", NamedTextColor.WHITE))
                .append(text(culledPlayers, NamedTextColor.WHITE))
                .append(text(" players", NamedTextColor.WHITE));
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        Set<PlatformPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());
        BossBar culling = BossBar.bossBar(text("-"), 1, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
        BossBar occlusion = BossBar.bossBar(text("-"), 1, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
        BossBar culled = BossBar.bossBar(text("-"), 1, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);

        runPerformance(ship, players, culling, occlusion, culled);

        return literal("performance")
                .requires(css -> css.getSender() instanceof PlatformPlayer
                        && css.getSender().hasPermission("playerculling.command.performance"))
                .executes(ctx -> {
                    PlatformPlayer sender = (PlatformPlayer) ctx.getSource().getSender();
                    if (players.contains(sender)) {
                        players.remove(sender);
                        sender.hideBossBar(culling);
                        sender.hideBossBar(occlusion);
                        sender.hideBossBar(culled);
                        sender.sendMessage(text("Disabled debug performance mode.", NamedTextColor.RED));
                    } else {
                        players.add(sender);
                        sender.showBossBar(culling);
                        sender.showBossBar(occlusion);
                        sender.showBossBar(culled);
                        sender.sendMessage(text("Enabled debug performance mode.", NamedTextColor.GREEN));
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static void runPerformance(
            CullShip ship, Set<PlatformPlayer> players,
            BossBar culling, BossBar occlusion, BossBar culled
    ) {

        ship.getPlatform().runTaskRepeatingAsync(new Runnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (players.isEmpty()) {
                    return;
                }

                double ms = ship.getLongestCullTime() / 1_000_000.0; // ns to ms

                if (++this.ticks % 10 == 0) {
                    culling.name(generatePerformanceInfo(ship, ms));
                    double progress = Math.max(0, Math.min(1, ms / (50)));
                    if (progress > 1) {
                        culling.color(BossBar.Color.RED);
                    } else if (progress > 0.66) {
                        culling.color(BossBar.Color.YELLOW);
                    } else if (progress > 0.33) {
                        culling.color(BossBar.Color.GREEN);
                    } else {
                        culling.color(BossBar.Color.BLUE);
                    }
                    culling.progress((float) progress);

                    occlusion.name(generateChunkCacheInfo(ship));
                }

                Set<CullPlayer> cullPlayers = ship.getPlayers();
                int culledPlayers = 0;
                for (CullPlayer cullPlayer : cullPlayers) {
                    culledPlayers += cullPlayer.getHiddenCount();
                }
                float culledPerPlayer = cullPlayers.size() <= 1 ? 0f : culledPlayers / (cullPlayers.size() * (cullPlayers.size() - 1f));
                culled.name(generateCulledPlayerInfo(culledPerPlayer, culledPlayers));
                culled.progress(Math.min(1, culledPerPlayer));
            }
        }, 0, 50L);
    }
}