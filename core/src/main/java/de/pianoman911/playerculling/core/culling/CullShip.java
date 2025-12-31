package de.pianoman911.playerculling.core.culling;


import de.pianoman911.playerculling.core.api.PlayerCullingApiImpl;
import de.pianoman911.playerculling.core.updater.PlayerCullingUpdater;
import de.pianoman911.playerculling.platformcommon.PlayerCullingConstants;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.util.atomics.AtomicCooldownRunnable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CullShip {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");

    private final IPlatform platform;
    private final YamlConfigHolder<PlayerCullingConfig> config;
    private final Map<UUID, CullPlayer> players = new HashMap<>();
    private final PlayerCullingUpdater updater;
    private final AtomicCooldownRunnable outerCullSkipWarn = new AtomicCooldownRunnable(PlayerCullingConstants.PANIC_LOG_INTERVAL);
    private final AtomicCooldownRunnable innerCullSkipWarn = new AtomicCooldownRunnable(PlayerCullingConstants.PANIC_LOG_INTERVAL);
    private final AtomicCooldownRunnable cullTimeWarn = new AtomicCooldownRunnable(PlayerCullingConstants.PANIC_LOG_INTERVAL);
    private ExecutorService executor;
    private boolean culling = true;
    private int cullWorkerCount = 0;

    public CullShip(IPlatform platform) {
        this.platform = platform;
        this.config = platform.loadConfig();
        this.updater = new PlayerCullingUpdater(this);

        platform.registerApi(new PlayerCullingApiImpl(this));
    }

    public void enable() {
        this.config.addReloadHookAndRun(new Consumer<>() {

            private int taskId = -1;

            @Override
            public void accept(PlayerCullingConfig config) {
                if (this.taskId != -1) {
                    CullShip.this.platform.cancelTask(this.taskId);
                }
                synchronized (CullShip.this.players) {
                    if (CullShip.this.executor != null) {
                        CullShip.this.executor.shutdown();
                    }

                    CullShip.this.executor = Executors.newFixedThreadPool(CullShip.this.config.getDelegate().scheduler.maxThreads,
                            new ThreadFactory() {

                                private final AtomicInteger threadCount = new AtomicInteger(0);

                                @Override
                                public Thread newThread(@NonNull Runnable r) {
                                    return new Thread(r, "PlayerCulling Worker " + this.threadCount.getAndIncrement());
                                }
                            });
                    long period = config.scheduler.maxCullTime;
                    this.taskId = CullShip.this.platform.runTaskRepeatingAsync(CullShip.this::cull, 1L, period);
                }
            }
        });
        this.updater.enable();
    }

    public Set<CullPlayer> getPlayers() {
        synchronized (this.players) {
            return Set.copyOf(this.players.values());
        }
    }

    public void forPlayers(Consumer<CullPlayer> consumer) {
        synchronized (this.players) {
            for (CullPlayer player : this.players.values()) {
                consumer.accept(player);
            }
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
    }


    public void removePlayer(UUID uniqueId) {
        synchronized (this.players) {
            this.players.remove(uniqueId);
        }
    }

    public long getLongestCullTime() {
        synchronized (this.players) {
            long longest = 0L;
            for (CullPlayer player : this.players.values()) {
                for (CullWorker cullWorker : player.getCullWorker()) {
                    long time = cullWorker.getAverageProcessingTime();
                    if (time > longest) {
                        longest = time;
                    }
                }
            }
            return longest;
        }
    }

    private long getCombinedLastRayStepCount() {
        synchronized (this.players) {
            long combined = 0L;
            for (CullPlayer player : this.players.values()) {
                for (CullWorker cullWorker : player.getCullWorker()) {
                    combined += cullWorker.getLastRayStepCount();
                }
            }
            return combined;
        }
    }

    public void toggleCulling(boolean enabled) {
        this.culling = enabled;
    }

    public String debugContainersFormat() {
        return "Worker: " + this.cullWorkerCount + " R: " + this.getCombinedLastRayStepCount() + "/s";
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

    private void cull() {
        if (!this.culling) {
            return;
        }
        long startNano = System.nanoTime();
        CountDownLatch latch;
        synchronized (this.players) {
            if (System.nanoTime() - startNano > this.config.getDelegate().scheduler.getMaxCullTimeNs()) {
                this.outerCullSkipWarn.run(() -> LOGGER.warn("Culling is falling behind! Skipping this cull cycle, culling could be too slow."));
                return;
            }
            this.cullWorkerCount = 0;
            for (CullPlayer player : this.players.values()) {
                player.prepareCull();
                this.cullWorkerCount += player.getCullWorker().size();
            }
            latch = new CountDownLatch(this.cullWorkerCount);
            for (CullPlayer player : this.players.values()) {
                for (CullWorker cullWorker : player.getCullWorker()) {
                    this.executor.submit(() -> {
                        long offsetNano = System.nanoTime() - startNano;
                        if (offsetNano > this.config.getDelegate().scheduler.maxCullTime * 1_000_000L) {
                            this.innerCullSkipWarn.run(() -> LOGGER.warn("Culling is falling behind! Current offset: {} ms. Skipping task," +
                                    " culling could be too slow.", offsetNano / 1_000_000L));
                            return;
                        }
                        try {
                            cullWorker.process(startNano);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }
        }
        try {
            // Wait double the max cull time to avoid blocking the scheduler thread indefinitely
            if (!latch.await(this.config.getDelegate().scheduler.maxCullTime * 2L, TimeUnit.MILLISECONDS)) {
                this.cullTimeWarn.run(() -> LOGGER.warn("Culling tasks did not finish in time! This could indicate a performance issue."));
            }
        } catch (InterruptedException ignored) {
        }
        synchronized (this.players) {
            for (CullPlayer value : this.players.values()) {
                value.finishCull();
            }
        }
    }
}

