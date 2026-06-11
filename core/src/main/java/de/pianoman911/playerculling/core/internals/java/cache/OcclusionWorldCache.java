package de.pianoman911.playerculling.core.internals.java.cache;

import de.pianoman911.playerculling.platformcommon.internals.ChunkCacheInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.ConcurrentLongCache;
import de.pianoman911.playerculling.platformcommon.util.StringUtil;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@NullMarked
public final class OcclusionWorldCache implements WorldCacheInterface {

    public static final ThreadPoolExecutor CACHE_EXECUTOR = (ThreadPoolExecutor) Executors.newCachedThreadPool(
            r -> new Thread(r, "Occlusion Cache Thread"));

    private final PlatformWorld<OcclusionWorldCache> world;
    private final ConcurrentLongCache<OcclusionChunkCache> chunks;

    public OcclusionWorldCache(PlatformWorld<OcclusionWorldCache> world) {
        this.world = world;
        this.chunks = new ConcurrentLongCache<>(key -> new OcclusionChunkCache(
                this, (int) (key >>> 32), (int) key));
    }

    public long bytes() {
        long bytes = 0;
        for (OcclusionChunkCache chunk : this.chunks) {
            bytes += chunk.byteSize();
        }
        return bytes;
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
