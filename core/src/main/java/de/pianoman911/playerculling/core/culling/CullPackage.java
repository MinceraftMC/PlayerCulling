package de.pianoman911.playerculling.core.culling;

import de.pianoman911.playerculling.core.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.core.provider.ChunkOcclusionDataProvider;
import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public class CullPackage {

    private final CullPlayer player;
    private final DataProvider provider;
    private final OcclusionCullingInstance cullingInstance;
    private final long[] timings = new long[5];
    private int timingIndex = 0;

    public CullPackage(CullPlayer player) {
        this.player = player;
        this.provider = new ChunkOcclusionDataProvider(this.player);
        this.provider.world(player.getPlatformPlayer().getWorld());
        this.cullingInstance = new OcclusionCullingInstance(this.provider);
    }

    public void process(long startNano) {
        synchronized (this) {
            while (this.player.tracked.hasEntries()) {
                PlatformEntity target = this.player.tracked.pop();
                if (target == null) {
                    continue;
                }
                this.player.cull(target, cullingInstance);
            }
            this.timings[this.timingIndex] = System.nanoTime() - startNano;
            this.timingIndex = (this.timingIndex + 1) % this.timings.length;
        }
    }

    public void world(PlatformWorld world) {
        this.provider.world(world);
    }

    public long getAverageProcessingTime() {
        long total = 0;
        for (long timing : this.timings) {
            total += timing;
        }
        return total / this.timings.length;
    }
}
