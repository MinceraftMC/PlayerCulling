package de.pianoman911.playerculling.core.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.playerculling.core.culling.CullContainer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSourceStack;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static de.pianoman911.playerculling.core.commands.PlayerCullingCommand.literal;
import static net.kyori.adventure.text.Component.text;

public class PlayerCullingContainerViewCommand {

    private PlayerCullingContainerViewCommand() {
    }

    public static LiteralArgumentBuilder<PlatformCommandSourceStack> getNode(CullShip ship) {
        Set<PlatformPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());
        Collection<CullContainerBar> bars = runPerformance(ship, players);

        return literal("viewcontainers")
                .requires(ctx -> ctx.getSender() instanceof PlatformPlayer && ctx.getSender().hasPermission("playerculling.command.viewcontainers"))
                .executes(ctx -> {
                    PlatformPlayer sender = (PlatformPlayer) ctx.getSource().getSender();
                    if (players.add(sender)) {
                        for (CullContainerBar bar : bars) {
                            sender.showBossBar(bar.bossBar());
                        }
                        sender.sendMessage(text("Enabled container view mode.", NamedTextColor.GREEN));
                    } else {
                        players.remove(sender);
                        for (CullContainerBar bar : bars) {
                            sender.hideBossBar(bar.bossBar());
                        }
                        sender.sendMessage(text("Disabled container view mode.", NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static Collection<CullContainerBar> runPerformance(
            CullShip ship, Set<PlatformPlayer> players
    ) {
        Int2ObjectMap<CullContainerBar> bossBars = new Int2ObjectArrayMap<>();
        ship.getPlatform().runTaskRepeatingAsync(new Runnable() {
            private final IntSet oldContainers = new IntArraySet();
            private int counter;

            @Override
            public void run() {
                if (players.isEmpty()) {
                    return;
                }
                this.counter++;
                if (counter > 4) {
                    this.counter = 0;
                }

                this.oldContainers.addAll(bossBars.keySet());
                for (CullContainer container : ship.getContainers()) {
                    bossBars.computeIfAbsent(container.getContainerId(), __ -> {
                                BossBar bar = BossBar.bossBar(text("Container " + container.getContainerId() + " - " + container.getAverageCullTime() +
                                                " - " + container.getLastRayStepCount() + " RaySteps/Tick"),
                                        0f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
                                for (PlatformPlayer player : players) {
                                    player.showBossBar(bar);
                                }
                                return new CullContainerBar(bar, container);
                            }
                    );
                    this.oldContainers.remove(container.getContainerId());
                }
                if (!this.oldContainers.isEmpty()) {
                    for (int oldContainer : this.oldContainers) {
                        CullContainerBar bar = bossBars.remove(oldContainer);
                        for (PlatformPlayer player : players) {
                            player.hideBossBar(bar.bossBar());
                        }
                    }
                    this.oldContainers.clear();
                }

                for (CullContainerBar cull : bossBars.values()) {
                    BossBar bossBar = cull.bossBar();
                    if (cull.container().getParkReason() != CullContainer.ParkReason.TIME_LEFT) { // parked due no players or disabled culling
                        float percentage = cull.container().getTtl() / (float) ship.getConfig().getDelegate().scheduler.getContainerTtlMs();
                        if (percentage < 0) { // Unsave to know -> on next clean up
                            percentage = this.counter > 2 ? 0 : 1;
                        }

                        bossBar.overlay(BossBar.Overlay.PROGRESS);
                        bossBar.progress(Math.min(1, Math.max(0, percentage)));
                        bossBar.name(text("Container " + cull.container().getContainerId() + " - Parked - TTL: " + cull.container().getTtl() / 1000 + "s"));
                        bossBar.color(BossBar.Color.BLUE);
                    } else {
                        float averageCullTime = (float) (cull.container().getAverageCullTime() / 1_000_000.0);
                        float percentage = averageCullTime / ship.getConfig().getDelegate().scheduler.maxCullTime;

                        bossBar.name(text("Container " + cull.container().getContainerId() + " - " + String.format("%.2f", averageCullTime) + "ms - " +
                                cull.container().getPlayerCount() + " players - " + cull.container().getLastRayStepCount() + " RaySteps/Tick"));
                        bossBar.progress(Math.min(1, Math.max(0, percentage)));
                        bossBar.color(percentage > 0.85f ? BossBar.Color.RED : percentage > 0.65f ? BossBar.Color.YELLOW : BossBar.Color.GREEN);
                        bossBar.overlay(BossBar.Overlay.NOTCHED_10);
                    }
                }
            }
        }, 0, 250L);

        return bossBars.values();
    }

    private record CullContainerBar(
            BossBar bossBar,
            CullContainer container
    ) {
    }
}
