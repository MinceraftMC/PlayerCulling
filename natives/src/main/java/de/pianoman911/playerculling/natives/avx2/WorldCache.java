package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativePart;
import de.pianoman911.playerculling.platformcommon.internals.ChunkCacheInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class WorldCache extends NativePart implements WorldCacheInterface {

    public static final int VOXEL_LENGTH = 2 * 2 * 2;
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");
    private static final MethodHandle HAS_CHUNK;
    private static final MethodHandle REMOVE_CHUNK;
    private static final MethodHandle INSERT_OR_UPDATE;
    private static final int MAX_CHUNK_COMPUTE_DEPTH = 5;

    static {
        HAS_CHUNK = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_world_cache_has_chunk"),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
                        ValueLayout.ADDRESS, // ChunkCache* instance
                        ValueLayout.JAVA_INT, // int32_t cx
                        ValueLayout.JAVA_INT // int32_t cz
                )
        );
        REMOVE_CHUNK = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_world_cache_remove_chunk"),
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, // ChunkCache* instance
                        ValueLayout.JAVA_INT, // int32_t cx
                        ValueLayout.JAVA_INT // int32_t cz
                )
        );
        INSERT_OR_UPDATE = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_world_cache_insert_or_update"),
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, // ChunkCache* instance
                        ValueLayout.JAVA_INT, // int32_t cx
                        ValueLayout.JAVA_INT, // int32_t cz,
                        ValueLayout.JAVA_INT, // int32_t minY,
                        ValueLayout.JAVA_INT, // int32_t maxY,
                        ValueLayout.ADDRESS // uint32_t* layers
                )
        );
    }

    private final PlatformWorld<WorldCache> world;
    private final LongSet computedChunks = new LongOpenHashSet();

    @SuppressWarnings("unchecked")
    public WorldCache(MemorySegment pointer, PlatformWorld<?> world) {
        super(pointer);
        this.world = (PlatformWorld<WorldCache>) world;
    }

    private static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xffffffffL);
    }

    public void ensureChunkComputed(int cx, int cz) {
        synchronized (this.computedChunks) {
            if (this.computedChunks.contains(chunkKey(cx, cz))) {
                return; // Already computed by another thread
            }
            this.computedChunks.add(chunkKey(cx, cz));
        }
        System.out.println("Computing chunk cache for chunk (" + cx + ", " + cz + ") in world " + this.world.getKey().asMinimalString());
        PlatformChunkAccess chunkAccess = this.world.getChunkAccess(cx, cz);
        computeChunk(chunkAccess, cx, cz, world.getMinY(), world.getMaxY(), 0);
    }

    public void computeChunk(PlatformChunkAccess chunkAccess, int cx, int cz, int minY, int maxY, int depth) {
        if (depth > MAX_CHUNK_COMPUTE_DEPTH) { // I don't know why, but this computation logic has some race conditions
            synchronized (this.computedChunks) {
                this.computedChunks.remove(chunkKey(cx, cz));
            }
            throw new RuntimeException("Exceeded max chunk compute depth, aborting. This should never happen and indicates a bug in the occlusion culling logic. Please report this to the developer.");
        }
        try {
            int height = maxY - minY + 1;
            int acx = cx << 4; // absolute chunk x in blocks
            int acz = cz << 4; // absolute chunk z in blocks

            int[] layers = new int[height * 32]; // 32 bits per layer, each bit represents a voxel (2x2x2 blocks)

            for (int rvy = 0; rvy < height; rvy++) { // rvy - relative voxel y
                int blockY = (rvy >> 1) + minY; // shift right by 1 to get block y, since 2 voxels per block
                int voxelY = rvy & 1; // voxel y inside block, 0 or 1

                for (int rvz = 0; rvz < 32; rvz++) {
                    int blockZ = (rvz >> 1) + acz; // shift right by 1 to get block z, since 2 voxels per block
                    int voxelZ = rvz & 1;

                    int arrayIndex = (rvy << 5) + rvz;
                    int bitRow = 0;

                    for (int rvx = 0; rvx < 32; rvx++) {
                        int blockX = (rvx >> 1) + acx;
                        int voxelX = rvx & 1;

                        int voxelIndex = (voxelZ << 2) | (voxelY << 1) | voxelX;

                        if (chunkAccess.isOpaque(blockX, blockY, blockZ, voxelIndex)) {
                            bitRow |= (1 << rvx);
                        }
                    }

                    layers[arrayIndex] |= bitRow;
                }
            }

            this.insertOrUpdate(cx, cz, minY, maxY, layers);
        } catch (Throwable exception) {
            LOGGER.warn("Error computing chunk cache for chunk ({}, {}) in world {}: {}", cx, cz, this.world.getKey().asMinimalString(), exception.getMessage(), exception);
            LOGGER.warn("Retrying chunk cache computation for chunk ({}, {}) in world {} (attempt {}/{})", cx, cz, this.world.getKey().asMinimalString(), depth + 1, MAX_CHUNK_COMPUTE_DEPTH);
            computeChunk(chunkAccess, cx, cz, minY, maxY, depth + 1);
        }
    }

    @Override
    public ChunkCacheInterface chunk(int cx, int cz) {
        throw new UnsupportedOperationException("Not supported in natives.");
    }

    @Override
    public boolean hasChunk(int cx, int cz) {
        try {
            return (boolean) HAS_CHUNK.invokeExact(this.getPointer(), cx, cz);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void removeChunk(int cx, int cz) {
        try {
            REMOVE_CHUNK.invokeExact(this.getPointer(), cx, cz);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    public void insertOrUpdate(int cx, int cz, int minY, int maxY, int[] layers) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeArray = arena.allocateFrom(ValueLayout.JAVA_INT, layers);

            INSERT_OR_UPDATE.invokeExact(this.getPointer(), cx, cz, minY, maxY, nativeArray);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }
}
