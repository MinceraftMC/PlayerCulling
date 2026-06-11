package de.pianoman911.playerculling.core.internals.java.cache;


import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.platformcommon.internals.DataProviderInterface;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ChunkOcclusionDataProvider implements DataProviderInterface {

    private static final int POS2CHUNK_SHIFT = 4 + 1;

    private final CullPlayer player;
    private @MonotonicNonNull PlatformWorld<OcclusionWorldCache> world;
    private @MonotonicNonNull OcclusionWorldCache cache;
    private @MonotonicNonNull OcclusionChunkCache chunk;

    public ChunkOcclusionDataProvider(CullPlayer player) {
        this.player = player;
    }

    @Override
    public final void updatePos(PlatformWorld<?> world, double  x, double y, double z) {
        PlatformWorld<OcclusionWorldCache> casted = (PlatformWorld<OcclusionWorldCache>) world;
        if (!world.equals(this.world)) {
            this.world = casted;
            this.cache = casted.getOcclusionWorldCache();
        }
    }

    private final void prepareChunk(int chunkX, int chunkZ) {
        OcclusionChunkCache chunk = this.chunk;
        if (chunk == null || chunk.x != chunkX || chunk.z != chunkZ) {
            this.chunk = this.cache.chunk(chunkX, chunkZ);
        }
    }

    @Override
    public final boolean isOpaqueFullCube(int x, int y, int z) {
        this.prepareChunk(x >> POS2CHUNK_SHIFT, z >> POS2CHUNK_SHIFT);
        return this.chunk.isVoxelOccluded(x, y, z);
    }

    @Override
    public final int getPlayerViewDistance() {
        if (this.world != null) {
            return this.world.getTrackingDistance(this.player.getPlatformPlayer());
        }
        return -1;
    }
}
