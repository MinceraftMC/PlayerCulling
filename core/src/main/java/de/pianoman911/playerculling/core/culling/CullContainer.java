package de.pianoman911.playerculling.core.culling;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.LockSupport;

public class CullContainer extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger("CullContainer");

    private final CullShip ship;
    private final Set<CullPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());
    private final int containerId;
    private final long[] times = new long[10]; // 10 last cull times
    private long lastPanic = 0L;
    private long lastSinceEmpty = Long.MAX_VALUE;
    private int index = 0;
    private ParkReason parkReason = ParkReason.NO_PLAYERS;
    private boolean culling = true;

    public CullContainer(int containerId, CullShip ship) {
        super("PlayerCulling-CullContainer-" + containerId);
        this.containerId = containerId;
        this.ship = ship;

        this.setDaemon(true);
        this.start();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            this.lastSinceEmpty = System.currentTimeMillis();
            try {
                if (!this.culling) {
                    this.parkReason = ParkReason.CULLING_DISABLED;
                    Arrays.fill(this.times, 0);
                    LockSupport.park();
                    continue;
                }
                long start = System.nanoTime();
                List<CullPlayer> players;
                synchronized (this.players) {
                    players = List.copyOf(this.players);
                }

                if (players.isEmpty()) {
                    Arrays.fill(this.times, 0);
                    this.parkReason = ParkReason.NO_PLAYERS;
                    LockSupport.park();
                    continue;
                }

                boolean forcePanic = this.cull(players, start);
                long end = System.nanoTime();
                long time = end - start;

                this.times[this.index++] = time;
                if (this.index == this.times.length) {
                    this.index = 0;
                }

                if (time > this.ship.getConfig().getDelegate().scheduler.getMaxCullTimeNs() || forcePanic) {
                    this.panic();
                }

                this.parkReason = ParkReason.TIME_LEFT;

                LockSupport.parkNanos(this.ship.getConfig().getDelegate().scheduler.getMaxCullTimeNs() - time);
            } catch (Throwable throwable) {
                LOGGER.error("Error in culling container", throwable);
            }
        }
    }

    private boolean cull(List<CullPlayer> players, long startTime) {
        for (CullPlayer player : players) {
            if (System.nanoTime() - startTime > this.ship.getConfig().getDelegate().scheduler.getMaxCullTimeNs()) {
                this.transferPlayer(player);
                return true;
            }
            if (player.getPlatformPlayer().isOnline()) {
                player.cull();
            } else {
                synchronized (this.players) {
                    this.players.remove(player);
                }
            }
        }
        return false;
    }

    private void panic() {
        if (this.players.size() < 2 // only 1 player in the container
                && this.lastPanic + 10000L > System.currentTimeMillis()) { // 10 seconds panic cooldown
            LOGGER.warn("Culling is to slow! Extreme panic, only 1 player is in the container, transferring is not worth...");
            this.lastPanic = System.currentTimeMillis();
            return;
        }
        CullPlayer first;
        boolean removed;
        synchronized (this.players) {
            first = this.players.iterator().next();
            removed = this.players.remove(first);
        }
        if (removed) {
            this.ship.addPlayer(first); // transfer player to another container
        }
    }

    private void transferPlayer(CullPlayer player) {
        synchronized (this.players) {
            this.players.remove(player);
        }
        this.ship.addPlayer(player);
    }

    public void mergeInto(CullContainer other) {
        synchronized (this.players) {
            for (CullPlayer player : this.players) {
                if (other != this) {
                    other.addPlayer0(player);
                }
            }
            this.players.clear();
        }
    }

    public void addPlayer(CullPlayer player) {
        synchronized (this.players) {
            this.addPlayer0(player);
        }
    }

    /**
     * Only call with synchronized(this.players)
     */
    private void addPlayer0(CullPlayer player) {
        this.players.add(player);
        if (this.parkReason == ParkReason.NO_PLAYERS) {
            LockSupport.unpark(this);
        }
    }

    public void toggleCulling(boolean enabled) {
        this.culling = enabled;
        if (enabled) {
            if (this.parkReason != ParkReason.NO_PLAYERS) {
                this.parkReason = ParkReason.TIME_LEFT;
                LockSupport.unpark(this);
            }
            return;
        }
        Set<CullPlayer> players;
        synchronized (this.players) {
            players = Set.copyOf(this.players);
        }
        for (CullPlayer player : players) {
            player.resetHidden();
        }
    }

    public long getAverageCullTime() {
        long sum = 0;
        for (long time : this.times) {
            sum += time;
        }
        return sum / this.times.length;
    }

    public int getPlayerCount() {
        synchronized (this.players) {
            return this.players.size();
        }
    }

    public long getLastRayStepCount(){
        long sum = 0;
        synchronized (this.players){
            for (CullPlayer player : this.players) {
                sum += player.getLastRaySteps();
            }
        }
        return sum;
    }

    public ParkReason getParkReason() {
        return this.parkReason;
    }

    public int getContainerId() {
        return this.containerId;
    }

    public long getTtl() {
        return this.lastSinceEmpty + this.ship.getConfig().getDelegate().scheduler.getContainerTtlMs() - System.currentTimeMillis();
    }

    public boolean isEmptyParked() {
        return this.parkReason == ParkReason.NO_PLAYERS && this.getPlayerCount() == 0;
    }

    public enum ParkReason {
        TIME_LEFT, // idle state
        NO_PLAYERS,
        CULLING_DISABLED
    }
}
