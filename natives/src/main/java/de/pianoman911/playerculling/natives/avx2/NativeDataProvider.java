package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.platformcommon.internals.DataProviderInterface;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class NativeDataProvider implements DataProviderInterface {

    private final DynamicWorld dynamicWorld;
    private final PlatformPlayer player;
    private @MonotonicNonNull PlatformWorld<WorldCache> world;
    private int ccx;
    private int ccz;
    private int chunkRadius;

    public NativeDataProvider(DynamicWorld dynamicWorld, PlatformPlayer player) {
        this.dynamicWorld = dynamicWorld;
        this.player = player;
    }

    @Override
    public final void updatePos(PlatformWorld<?> world, double x, double y, double z) {
        this.world = (PlatformWorld<WorldCache>) world;
        int ccx = (int) x >> 4;
        int ccz = (int) z >> 4;
        if (this.ccx == ccx && this.ccz == ccz) {
            return;
        }
        this.ccx = ccx;
        this.ccz = ccz;
        // Trigger chunk grid update

        // ensure chunks cached
        for (int cx = 0; cx < (chunkRadius * 2) + 1; cx++) {
            for (int cz = 0; cz < (chunkRadius * 2) + 1; cz++) {
                this.world.getOcclusionWorldCache().ensureChunkComputed(ccx - chunkRadius + cx, ccz - chunkRadius + cz);
            }
        }

        this.dynamicWorld.updateGrid(this.ccx, this.ccz, this.world.getOcclusionWorldCache());
    }

    @Override
    public final boolean isOpaqueFullCube(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported with natives");
    }

    @Override
    public final int getPlayerViewDistance() {
        if (this.world != null) {
            int trackingDistanceBlocks = this.world.getTrackingDistance(this.player);
            int trackingDistanceChunks = trackingDistanceBlocks >> 4;
            if (trackingDistanceChunks != this.chunkRadius){
                this.chunkRadius = trackingDistanceChunks;
                this.dynamicWorld.resize(this.chunkRadius);
            }
            return trackingDistanceBlocks;
        }
        return -1;
    }
}
