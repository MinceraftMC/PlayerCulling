package de.pianoman911.playerculling.core.culling;


import de.pianoman911.playerculling.core.api.PlayerCullingApiImpl;
import de.pianoman911.playerculling.core.updater.PlayerCullingUpdater;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * The Ship holds {@link CullContainer} instances and distributes players to them.
 * Each {@link CullContainer} represent a thread
 */
public class CullShip {

    private static final long PANIC_LOG_INTERVAL = 10000L; // 10 seconds
    private static final Logger LOGGER = LoggerFactory.getLogger("CullShip");

    private final IPlatform platform;
    private final YamlConfigHolder<PlayerCullingConfig> config;
    private final List<CullContainer> containers;
    private final Map<UUID, CullPlayer> players = new HashMap<>();
    private final PlayerCullingUpdater updater;
    private int lastContainer = 1;
    private long lastPanicLog = 0L;
    private boolean culling = true;

    public CullShip(IPlatform platform) {
        this.platform = platform;
        this.config = platform.loadConfig();
        this.updater = new PlayerCullingUpdater(this);

        this.containers = new ArrayList<>(this.config.getDelegate().scheduler.maxThreads);
        this.containers.add(new CullContainer(this.lastContainer++, this));

        platform.registerApi(new PlayerCullingApiImpl(this));
    }

    public void enable() {
        this.config.addReloadHookAndRun(new Consumer<>() {

            private int taskId = -1;

            @Override
            public void accept(PlayerCullingConfig config) {
                platform.cancelTask(this.taskId);
                this.taskId = platform.runTaskRepeatingAsync(() -> CullShip.this.cleanContainers(false), 0L,
                        config.scheduler.getCleanupIntervalMs());
            }
        });
        this.updater.enable();
    }

    private CullContainer createContainer() {
        CullContainer container = new CullContainer(this.lastContainer++, this);
        this.containers.add(container);

        LOGGER.info("Added new container: {}", container.getName());
        return container;
    }

    private CullContainer getLowestLoadedContainer() {
        CullContainer lowest;
        CullContainer lowestParked = null;
        synchronized (this.containers) {
            if (this.containers.isEmpty()) {
                return this.createContainer();
            }
            lowest = this.containers.getFirst();

            for (CullContainer container : this.containers) {
                if (container.getAverageCullTime() < lowest.getAverageCullTime()) {
                    if (container.isEmptyParked()) { // Prefer non-parked containers
                        lowestParked = container;
                    } else {
                        lowest = container;
                    }
                }
            }

            if (lowest.getAverageCullTime() > this.config.getDelegate().scheduler.getMaxTransferNs()) { // If the lowest container is overloaded, we need to create a new one
                if (lowestParked == null) {
                    if (this.containers.size() < this.config.getDelegate().scheduler.maxThreads) {
                        lowest = this.createContainer();
                    } else if (this.lastPanicLog + PANIC_LOG_INTERVAL < System.currentTimeMillis()) { // Don't spam the log
                        LOGGER.warn("All containers are overloaded, thread limit reached. Culling skip will be caused.");
                        this.lastPanicLog = System.currentTimeMillis();
                    }
                } else { // Use the parked container if available
                    lowest = lowestParked;
                }
            }
        }
        return lowest;
    }

    @Unmodifiable
    public List<CullContainer> getContainers() {
        synchronized (this.containers) {
            return List.copyOf(this.containers);
        }
    }

    public Set<CullPlayer> getPlayers() {
        synchronized (this.players) {
            return Set.copyOf(this.players.values());
        }
    }

    public CullPlayer getPlayer(UUID playerId) {
        synchronized (this.players) {
            return this.players.get(playerId);
        }
    }

    public void addPlayer(CullPlayer player) {
        if (!player.getPlatformPlayer().isOnline()) {
            return; // skip adding player if they are disconnected
        }
        synchronized (this.players) {
            this.players.put(player.getPlatformPlayer().getUniqueId(), player);
        }
        CullContainer container = this.getLowestLoadedContainer();
        container.addPlayer(player);
    }

    public void removePlayer(UUID uniqueId) {
        synchronized (this.players) {
            this.players.remove(uniqueId);
        }
    }

    public long getLongestCullTime() {
        synchronized (this.containers) {
            long longest = 0;
            for (CullContainer container : this.containers) {
                longest = Math.max(longest, container.getAverageCullTime());
            }
            return longest;
        }
    }

    public long getCombinedLastRayStepCount() {
        long sum = 0;
        synchronized (this.containers) {
            for (CullContainer container : this.containers) {
                sum += container.getLastRayStepCount();
            }
        }
        return sum;
    }

    public void toggleCulling(boolean enabled) {
        this.culling = enabled;
        synchronized (this.containers) {
            for (CullContainer container : this.containers) {
                container.toggleCulling(enabled);
            }
        }
    }

    public String debugContainersFormat() {
        int running = 0;
        int parked = 0;
        synchronized (this.containers) {
            for (CullContainer container : this.containers) {
                if (container.getParkReason() == CullContainer.ParkReason.TIME_LEFT) {
                    running++;
                } else {
                    parked++;
                }
            }
        }
        return "Containers R: " + running + " P: " + parked + " T: " + this.containers.size() + " R: " + this.getCombinedLastRayStepCount();
    }

    public boolean isCullingEnabled() {
        return this.culling;
    }

    public YamlConfigHolder<PlayerCullingConfig> getConfig() {
        return this.config;
    }

    public IPlatform getPlatform() {
        return this.platform;
    }

    public PlayerCullingUpdater getUpdater() {
        return this.updater;
    }

    public int cleanContainers(boolean force) {
        AtomicInteger cleaned = new AtomicInteger();
        synchronized (this.containers) {
            // Merge containers on low load
            this.containers.sort(Comparator.comparing(CullContainer::getPlayerCount).reversed() // Free containers at the end
                    .thenComparingLong(CullContainer::getAverageCullTime)); // Sort containers by average cull time
            if (this.containers.size() >= 2) {
                CullContainer first = this.containers.getFirst();
                CullContainer second = this.containers.get(1);
                if (second.getPlayerCount() > 0) { // Only merge full containers - prevent swapping into parked containers
                    if (first.getAverageCullTime() > second.getAverageCullTime()) { // Swap if first is greater than second
                        CullContainer temp = first;
                        first = second;
                        second = temp;
                    }
                    long combinedCullTime = first.getAverageCullTime() + second.getAverageCullTime();
                    if (combinedCullTime < this.config.getDelegate().scheduler.getMaxMergeNs()) { // Combined under factor of max cull time
                        first.mergeInto(second); // Merge lighter container into heavier one
                        second.mergeInto(first);
                        LOGGER.info("Container {} merged into {}", second.getName(), first.getName());
                        this.platform.runTaskLaterAsync(() -> this.cleanContainers(force), 1000L); // Recheck second merge after 1 second
                    }
                }
            }

            // Remove empty containers
            this.containers.removeIf(container -> {
                if (this.containers.size() <= 1) { // Don't remove the last container
                    return false;
                }
                if (container.getParkReason() == CullContainer.ParkReason.NO_PLAYERS && (force || container.getTtl() <= 0)) {
                    container.interrupt();
                    LockSupport.unpark(container);
                    try {
                        container.join(5000L);
                        LOGGER.info("Container {} cleaned up", container.getName());
                    } catch (InterruptedException exception) {
                        LOGGER.error("Failed to stop container", exception);
                    }
                    cleaned.getAndIncrement();
                    return true;
                }
                return false;
            });
        }
        return cleaned.get();
    }
}

