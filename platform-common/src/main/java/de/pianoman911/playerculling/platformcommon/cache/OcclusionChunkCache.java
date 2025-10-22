package de.pianoman911.playerculling.platformcommon.cache;


import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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
    private volatile long @MonotonicNonNull [] occlusionData = null;
    private volatile int minY;
    private volatile int minYX2;
    private volatile int maxY;
    private volatile int height;
    private volatile int heightX2;
    private volatile boolean fullyComputed = false;
    private volatile boolean computing = false;

    private @Nullable WeakReference<PlatformChunkAccess> chunk;

    // Block structure:
    // 8 bits: 2x2x2 shape for each block corner (stairs, slabs, etc.)
    // The Chunk Size is 16xYx16 -> Y is variable

    // The long array stores 8 blocks in a row, so 8 blocks in the x direction
    public OcclusionChunkCache(OcclusionWorldCache world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;

        this.minY = world.getWorld().getMinY();
        this.minYX2 = this.minY * 2;
        this.maxY = world.getWorld().getMaxY();
        this.height = this.maxY - this.minY;
        this.heightX2 = this.height * 2;

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

    public final boolean isOccluded(int index) {
        return (this.occlusionData[index >>> 6] &  // index / 64 for long array index
                (1L << (index & 63))) != 0; // index % 64 for bit index inside long
    }

    public final boolean isOccluded(double x, double y, double z) {
        y -= this.minY;
        if (y < 0 || y >= this.height) {
            return false;
        }

        int relX = Math.floorMod((int) (x * 2d), 16 * 2);
        int relZ = Math.floorMod((int) (z * 2d), 16 * 2);

        if (this.fullyComputed) { // Fully computed -> use cache
            int index = index(relX, (int) (y * 2d), relZ);
            return this.isOccluded(index);
        }

        PlatformChunkAccess chunk = this.resolveChunkAccess();
        if (chunk == null) {
            return false;
        }

        for (int i = 0; i < VOXEL_LENGTH; i++) {
            if (chunk.isOpaque(relX >> 1, (int) y + this.minY, relZ >> 1, i)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isVoxelOccluded(int x, int y, int z) {
        y -= this.minYX2;
        if (y < 0 || y >= this.heightX2) {
            return false;
        }
        if (this.fullyComputed) {
            return this.isOccluded(index(x & 31, y, z & 31));
        }
        return this.isOccluded(x / 2d, (y + this.minYX2) / 2d, z / 2d); // Only called if no cache is available
    }

    void computeFully(@Nullable PlatformChunkAccess access) {
        synchronized (this) {
            if (access == null || this.fullyComputed || this.computing) {
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
            this.computing = false;
            return;
        }
        try {
            int bottomY = this.height;
            int topY = 0;
            long[] data = new long[this.height * 4 * 8];

            for (int cy = 0; cy < this.height; cy++) {
                int y = cy + this.minY;
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

            int height = topY - bottomY + 1;
            if (height != this.height) {
                long[] minifiedData = new long[height * 4 * 8]; // resize data array to save memory
                System.arraycopy(data, (bottomY - this.minY) * 4 * 8, minifiedData, 0, height * 4 * 8);
                this.occlusionData = minifiedData;
            } else {
                this.occlusionData = data;
            }

            this.minY = bottomY;
            this.minYX2 = bottomY * 2;
            this.maxY = topY;
            this.height = height;
            this.heightX2 = height * 2;

            this.fullyComputed = true;
            this.computing = false;
        } catch (Throwable throwable) {
            LOGGER.error("Failed to compute occlusion data for chunk at {} {}", this.x, this.z, throwable);
            this.computeFully0(access, depth + 1); // retry
        }
    }

    public void recalculateBlock(int x, int y, int z) {
        if (!this.fullyComputed) {
            return;
        }

        x = x & 15;
        z = z & 15;
        y -= this.minY;
        if (y < 0 || y >= this.height) { // out of range -> recalculate
            int prevMin = this.minY;
            int prevMax = this.maxY;
            this.minY = Math.min(prevMin, y + prevMin);
            this.maxY = Math.max(prevMax, y + prevMin);
            int height = this.maxY - this.minY + 1;

            long[] data = new long[height * 4 * 8];
            System.arraycopy(this.occlusionData, (prevMin - this.minY) * 4 * 8, data, (prevMin - this.minY) * 4 * 8, (prevMax - prevMin + 1) * 4 * 8);
            this.occlusionData = data;
            this.height = height;
            this.heightX2 = height * 2;
            this.minYX2 = this.minY * 2;
        }
        PlatformChunkAccess chunk = this.resolveChunkAccess();
        if (chunk == null) {
            return; // chunk is unloaded
        }

        for (int i = 0; i < 8; i++) {
            boolean opaque = chunk.isOpaque(x, y + this.minY, z, i);
            double ix = x + (i & 1) * 0.5;
            double iy = y + ((i >> 1) & 1) * 0.5;
            double iz = z + ((i >> 2) & 1) * 0.5;
            set(this.occlusionData, doubleDoubleIndex(ix, iy, iz), opaque);
        }
    }

    public OcclusionWorldCache getWorld() {
        return this.world;
    }

    final long[] getOcclusionData() {
        return this.occlusionData;
    }

    public final int getX() {
        return this.x;
    }

    public final int getZ() {
        return this.z;
    }

    public final int getHeight() {
        return this.height;
    }

    public final int getMinY() {
        return this.minY;
    }

    public final int getMaxY() {
        return this.maxY;
    }

    public boolean isFullyComputed() {
        return this.fullyComputed;
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
        if (this.occlusionData != null) {
            size += this.occlusionData.length * Long.BYTES;
        }
        return size;
    }
}
