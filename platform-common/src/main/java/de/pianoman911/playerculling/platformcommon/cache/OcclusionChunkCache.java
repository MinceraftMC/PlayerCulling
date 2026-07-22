package de.pianoman911.playerculling.platformcommon.cache;


import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

@NullMarked
public final class OcclusionChunkCache {

    public static final int VOXEL_LENGTH = 2 * 2 * 2;
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");
    private static final int MAX_CHUNK_COMPUTE_DEPTH = 5;
    public final int x;
    public final int z;
    private final OcclusionWorldCache world;
    private final int worldMinY;
    private final int worldMaxY;
    private final int worldHeight;
    private volatile CacheState state;
    private volatile boolean computing = false;
    private long modificationVersion = 0L;

    private @Nullable WeakReference<PlatformChunkAccess> chunk;

    // Block structure:
    // 8 bits: 2x2x2 shape for each block corner (stairs, slabs, etc.)
    // The Chunk Size is 16xYx16 -> Y is variable

    // The long array stores 8 blocks in a row, so 8 blocks in the x direction
    public OcclusionChunkCache(OcclusionWorldCache world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;

        this.worldMinY = world.getWorld().getMinY();
        this.worldMaxY = world.getWorld().getMaxY();
        this.worldHeight = this.worldMaxY - this.worldMinY;

        this.state = this.uncomputedState();

        this.resolveChunkAccess(); // Trigger cache building
    }

    public static void set(long[] data, int index, boolean occludes) {
        int i = index >>> 6; // index / 64 for long array index
        int j = index & 63; // index % 64 for bit index inside long
        if (occludes) {
            data[i] |= 1L << j;
        } else {
            data[i] &= ~(1L << j);
        }
    }

    public static int index(int x, int y, int z) {
        return x | (z << 5) | (y << 10);
    }

    private @Nullable PlatformChunkAccess resolveChunkAccess() {
        PlatformChunkAccess chunk = this.chunk != null ? this.chunk.get() : null;
        if (chunk == null) {
            chunk = this.world.getWorld().getChunkAccess(this.x, this.z);
            if (chunk == null) {
                return null;
            }
            this.chunk = new WeakReference<>(chunk);

            this.computeFully(chunk);
        }
        return chunk;
    }

    private final int doubleDoubleIndex(double x, double y, double z) {
        int ix = (int) (x * 2);
        int iy = (int) (y * 2);
        int iz = (int) (z * 2);
        return index(ix, iy, iz);
    }

    private static boolean isOccluded(long[] data, int index) {
        return (data[index >>> 6] &  // index / 64 for long array index
                (1L << (index & 63))) != 0; // index % 64 for bit index inside long
    }

    public final boolean isOccluded(int index) {
        long[] data = this.state.data;
        return data != null && isOccluded(data, index);
    }

    public final boolean isOccluded(double x, double y, double z) {
        CacheState state = this.state;
        y -= state.minY;
        if (y < 0 || y >= state.height) {
            return false;
        }

        int relX = Math.floorMod((int) (x * 2d), 16 * 2);
        int relZ = Math.floorMod((int) (z * 2d), 16 * 2);

        if (state.data != null) { // Fully computed -> use cache
            int index = index(relX, (int) (y * 2d), relZ);
            return isOccluded(state.data, index);
        }

        PlatformChunkAccess chunk = this.resolveChunkAccess();
        if (chunk == null) {
            return false;
        }

        for (int i = 0; i < VOXEL_LENGTH; i++) {
            if (chunk.isOpaque(relX >> 1, (int) y + state.minY, relZ >> 1, i)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isVoxelOccluded(int x, int y, int z) {
        CacheState state = this.state;
        int minYX2 = state.minY * 2;
        y -= minYX2;
        if (y < 0 || y >= state.height * 2) {
            return false;
        }
        if (state.data != null) {
            return isOccluded(state.data, index(x & 31, y, z & 31));
        }
        return this.isOccluded(x / 2d, (y + minYX2) / 2d, z / 2d); // Only called if no cache is available
    }

    void computeFully(@Nullable PlatformChunkAccess access) {
        synchronized (this) {
            if (access == null || this.state.data != null || this.computing) {
                return;
            }
            this.computing = true;
        }
        OcclusionWorldCache.CACHE_EXECUTOR.execute(() -> this.computeFully0(access, 0));
    }

    private void computeFully0(PlatformChunkAccess access, int depth) {
        if (depth > MAX_CHUNK_COMPUTE_DEPTH) {
            LOGGER.warn("Failed to compute occlusion data for chunk at {}, {} after "
                    + MAX_CHUNK_COMPUTE_DEPTH + " retries", this.x, this.z);
            synchronized (this) {
                this.computing = false;
            }
            return;
        }
        try {
            long startVersion;
            synchronized (this) {
                startVersion = this.modificationVersion;
            }

            int bottomY = this.worldMaxY;
            int topY = this.worldMinY - 1;
            long[] data = new long[this.worldHeight * 4 * 8];

            for (int cy = 0; cy < this.worldHeight; cy++) {
                int y = cy + this.worldMinY;
                for (int cx = 0; cx < 16; cx++) {
                    for (int cz = 0; cz < 16; cz++) {
                        for (int i = 0; i < VOXEL_LENGTH; i++) {
                            boolean opaque = access.isOpaque(cx, y, cz, i);
                            if (opaque) {
                                bottomY = Math.min(bottomY, y);
                                topY = Math.max(topY, y);
                                double offX = (i & 1) * 0.5;
                                double offY = ((i >> 1) & 1) * 0.5;
                                double offZ = ((i >> 2) & 1) * 0.5;

                                set(data, this.doubleDoubleIndex(cx + offX, cy + offY, cz + offZ), true);
                            }
                        }
                    }
                }
            }

            int height = Math.max(0, topY - bottomY + 1);
            long[] computedData;
            if (height == 0) {
                computedData = new long[0];
                bottomY = this.worldMinY;
                topY = this.worldMinY - 1;
            } else if (height != this.worldHeight) {
                long[] minifiedData = new long[height * 4 * 8]; // resize data array to save memory
                System.arraycopy(data, (bottomY - this.worldMinY) * 4 * 8, minifiedData, 0, height * 4 * 8);
                computedData = minifiedData;
            } else {
                computedData = data;
            }

            boolean retry;
            synchronized (this) {
                retry = startVersion != this.modificationVersion;
                if (!retry) {
                    this.state = new CacheState(computedData, bottomY, topY, height);
                    this.computing = false;
                }
            }
            if (retry) {
                this.computeFully0(access, depth + 1);
            }
        } catch (Throwable throwable) {
            LOGGER.error("Failed to compute occlusion data for chunk at {} {}", this.x, this.z, throwable);
            this.computeFully0(access, depth + 1); // retry
        }
    }

    public void recalculateBlock(int x, int y, int z) {
        PlatformChunkAccess chunk = this.resolveChunkAccess();
        if (chunk == null) {
            return; // chunk is unloaded
        }

        boolean recompute = false;
        synchronized (this) {
            this.modificationVersion++;
            CacheState state = this.state;
            if (state.data == null) {
                return;
            }

            x &= 15;
            z &= 15;
            y -= state.minY;
            if (y < 0 || y >= state.height) {
                // The minified cache cannot be resized safely while culling threads are reading it.
                // Fall back to live chunk data until a new complete snapshot has been published.
                this.state = this.uncomputedState();
                recompute = true;
            } else {
                for (int i = 0; i < VOXEL_LENGTH; i++) {
                    boolean opaque = chunk.isOpaque(x, y + state.minY, z, i);
                    double ix = x + (i & 1) * 0.5;
                    double iy = y + ((i >> 1) & 1) * 0.5;
                    double iz = z + ((i >> 2) & 1) * 0.5;
                    set(state.data, doubleDoubleIndex(ix, iy, iz), opaque);
                }
                // Publish the writes to the array through the volatile snapshot field.
                this.state = new CacheState(state.data, state.minY, state.maxY, state.height);
            }
        }

        if (recompute) {
            this.computeFully(chunk);
        }
    }

    private CacheState uncomputedState() {
        return new CacheState(null, this.worldMinY, this.worldMaxY - 1, this.worldHeight);
    }

    public OcclusionWorldCache getWorld() {
        return this.world;
    }

    final long[] getOcclusionData() {
        return this.state.data;
    }

    public final int getX() {
        return this.x;
    }

    public final int getZ() {
        return this.z;
    }

    public final int getHeight() {
        return this.state.height;
    }

    public final int getMinY() {
        return this.state.minY;
    }

    public final int getMaxY() {
        return this.state.maxY;
    }

    public boolean isFullyComputed() {
        return this.state.data != null;
    }

    public final boolean[] isOpaqueFullBlock(int x, int y, int z) {
        boolean[] shapes = new boolean[VOXEL_LENGTH];
        PlatformChunkAccess chunk = this.resolveChunkAccess();
        if (chunk == null) {
            return OcclusionMappings.EMPTY_CUBE;
        }
        for (int i = 0; i < shapes.length; i++) {
            shapes[i] = chunk.isOpaque(x, y, z, i);
        }

        return shapes;
    }

    public final int byteSize() {
        int size = 21; // 21 bytes for the chunk cache object itself
        long[] data = this.state.data;
        if (data != null) {
            size += data.length * Long.BYTES;
        }
        return size;
    }

    private record CacheState(long @Nullable [] data, int minY, int maxY, int height) {
    }
}
