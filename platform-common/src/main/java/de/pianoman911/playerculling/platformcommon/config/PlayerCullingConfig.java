package de.pianoman911.playerculling.platformcommon.config;

import de.pianoman911.playerculling.platformcommon.util.WaypointMode;
import org.jspecify.annotations.NullUnmarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@NullUnmarked
public final class PlayerCullingConfig {

    public int configVersion = 1;
    public Scheduler scheduler = new Scheduler();
    public Updater updater = new Updater();
    public WaypointMode waypointMode = WaypointMode.HIDDEN;

    @ConfigSerializable
    public final static class Scheduler {

        public int maxThreads = Runtime.getRuntime().availableProcessors() / 3;
        public int maxCullTime = 45; // ms
        public double forkThresholdFactor = 0.7;
        public double destroyThresholdFactor = 0.2;

        public long getMaxCullTimeNs() {
            return this.maxCullTime * 1_000_000L;
        }

        public long getForkThresholdNs() {
            return (long) (getMaxCullTimeNs() * this.forkThresholdFactor);
        }

        public long getDestroyThresholdNs() {
            return (long) (getMaxCullTimeNs() * this.destroyThresholdFactor);
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
