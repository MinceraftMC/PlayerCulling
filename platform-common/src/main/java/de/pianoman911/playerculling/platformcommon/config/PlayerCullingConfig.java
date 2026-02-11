package de.pianoman911.playerculling.platformcommon.config;

import de.pianoman911.playerculling.platformcommon.util.WaypointMode;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullUnmarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Set;

@ConfigSerializable
@NullUnmarked
public final class PlayerCullingConfig {

    public int configVersion = 1;
    public Scheduler scheduler = new Scheduler();
    public Culling culling = new Culling();
    public Updater updater = new Updater();
    public WaypointMode waypointMode = WaypointMode.HIDDEN;

    @ConfigSerializable
    public final static class Scheduler {

        public int maxThreads = Runtime.getRuntime().availableProcessors() / 3;
        public int cleanupInterval = 30; // sec
        public int containerTtl = 30; // sec
        public int maxCullTime = 45; // ms
        public double maxTransferFactor = 0.7;
        public double maxMergeFactor = 0.5;

        public long getMaxCullTimeNs() {
            return this.maxCullTime * 1_000_000L;
        }

        public long getCleanupIntervalMs() {
            return this.cleanupInterval * 1_000L;
        }

        public long getContainerTtlMs() {
            return this.containerTtl * 1_000L;
        }

        public double getMaxTransferNs() {
            return this.maxTransferFactor * this.getMaxCullTimeNs();
        }

        public double getMaxMergeNs() {
            return this.maxMergeFactor * this.getMaxCullTimeNs();
        }
    }

    @ConfigSerializable
    public final static class Culling {

        public boolean ignoreNametags = false;
        public int beginCullDistance = 0;
        public Set<Key> blacklistedWorlds = Set.of();

        public int getBeginCullDistanceSquared() {
            return this.beginCullDistance * this.beginCullDistance;
        }
    }

    @ConfigSerializable
    public final static class Updater {

        public boolean enabled = true;
        public boolean notifyAdmins = true;
        public int intervalHours = 24; // hours

        public long getIntervalMs() {
            return this.intervalHours * 3_600_000L; // 60min * 60sec * 1000ms
        }
    }
}
