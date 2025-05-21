package de.pianoman911.playerculling.platformcommon.cache;

import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.ConcurrentLongCache;
import de.pianoman911.playerculling.platformcommon.util.StringUtil;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@NullMarked
public final class OcclusionWorldCache {

    public static final ThreadPoolExecutor CACHE_EXECUTOR = (ThreadPoolExecutor) Executors.newCachedThreadPool(
            r -> new Thread(r, "Occlusion Cache Thread"));

    private final PlatformWorld world;
    private final ConcurrentLongCache<OcclusionChunkCache> chunks;

    public OcclusionWorldCache(PlatformWorld world) {
        this.world = world;
        this.chunks = new ConcurrentLongCache<>(key -> new OcclusionChunkCache(
                this, (int) (key >>> 32), (int) key));
    }

    public static int chunksStored(Collection<PlatformWorld> worlds) {
        int chunksStored = 0;
        for (PlatformWorld world : worlds) {
            chunksStored += world.getOcclusionWorldCache().chunks.size();
        }
        return chunksStored;
    }

    public static long byteSize(Collection<PlatformWorld> worlds) {
        long bytes = 0;
        for (PlatformWorld world : worlds) {
            bytes += world.getOcclusionWorldCache().bytes();
        }
        return bytes;
    }

    public static String formattedByteSize(Collection<PlatformWorld> worlds) {
        return StringUtil.toNumInUnits(byteSize(worlds));
    }

    public long bytes() {
        long bytes = 0;
        for (OcclusionChunkCache chunk : this.chunks) {
            bytes += chunk.byteSize();
        }
        return bytes;
    }

    public boolean isOccluding(int x, int y, int z) {
        return chunk(x >> 4, z >> 4).isOccluded(x, y, z);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.chunks.get(((long) chunkX << 32 | (long) chunkZ & 0xFFFFFFFFL)) != null;
    }

    public final OcclusionChunkCache chunk(int chunkX, int chunkZ) {
        return this.chunks.getOrCompute((long) chunkX << 32 | (long) chunkZ & 0xFFFFFFFFL);
    }

    public void removeChunk(OcclusionChunkCache cache) {
        this.removeChunk(cache.getX(), cache.getZ());
    }

    public void removeChunk(int chunkX, int chunkZ) {
        this.chunks.remove(((long) chunkX << 32 | (long) chunkZ & 0xFFFFFFFFL));
    }

    public ConcurrentLongCache<OcclusionChunkCache> getChunkCache() {
        return this.chunks;
    }

    public PlatformWorld getWorld() {
        return this.world;
    }
}
