package de.pianoman911.playerculling.core.culling;


import de.pianoman911.playerculling.core.api.PlayerCullingApiImpl;
import de.pianoman911.playerculling.core.updater.PlayerCullingUpdater;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CullShip {

    private static final long PANIC_LOG_INTERVAL = 10000L; // 10 seconds
    private static final Logger LOGGER = LoggerFactory.getLogger("CullShip");

    private final IPlatform platform;
    private final YamlConfigHolder<PlayerCullingConfig> config;
    private final Map<UUID, CullPlayer> players = new HashMap<>();
    private final PlayerCullingUpdater updater;
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

                    CullShip.this.executor = Executors.newFixedThreadPool(CullShip.this.config.getDelegate().scheduler.maxThreads);
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
            this.cullWorkerCount = 0;
            for (CullPlayer player : this.players.values()) {
                player.prepareCull();
                this.cullWorkerCount += player.getCullWorker().size();
            }
            latch = new CountDownLatch(this.cullWorkerCount);
            for (CullPlayer player : this.players.values()) {
                for (CullWorker cullWorker : player.getCullWorker()) {
                    this.executor.submit(() -> {
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
            if (!latch.await(10, TimeUnit.SECONDS)) {
                LOGGER.warn("Culling tasks did not complete within the timeout");
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

