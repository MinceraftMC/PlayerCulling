package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

@NullMarked
public final class OcclusionMappings {

    public static final boolean[] EMPTY_CUBE = new boolean[]{false, false, false, false, false, false, false, false};
    public static final boolean[] FULL_CUBE = new boolean[]{true, true, true, true, true, true, true, true};

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");

    private final long[] occlusionMappings; // 8 x 8 bits (2x2x2 booleans)
    private final int maxSize;
    private boolean built;

    public OcclusionMappings(int size) {
        this.occlusionMappings = new long[(int) Math.ceil(size / 8d)];
        this.maxSize = size;
    }

    // if the cache hasn't been built yet, it will always return false;
    // this shouldn't matter though, as this shouldn't prevent players from being culled
    public boolean hasVoxel(int blockId, int index) {
        // lookup mapping blob
        long voxelData = this.occlusionMappings[blockId >> 3];
        // calculate mask, shift by remaining block id bits
        // and additionally shift by index in original boolean array
        long mask = 1L << (index + ((blockId & 7L) << 3L));
        // check bit at mask
        return (voxelData & mask) != 0;
    }

    // lazy build cache if not built yet
    public void lazyBuildCache(Function<Integer, boolean[]> mapper) {
        if (this.built) {
            return;
        }
        long start = System.nanoTime();
        this.buildCache(mapper);
        long time = System.nanoTime() - start;

        long bytes = (long) Long.BYTES * this.occlusionMappings.length;
        LOGGER.info("Occlusion mappings cache built with {} voxels and {} entries ({}) in {}ms",
                this.maxSize, this.occlusionMappings.length,
                StringUtil.toNumInUnits(bytes), time / 1_000_000d);
    }

    public void buildCache(Function<Integer, boolean[]> mapper) {
        // ensure no stuff at the end gets skipped
        int maxLength = this.occlusionMappings.length;
        // iterate through each final index
        for (int i = 0; i < maxLength; i += 64 / 8) {
            // calculate bits in correct order so it's
            // very easy + fast to retrieve them again
            long bits = 0L;
            for (int j = i + 8 - 1; j >= i; j--) {
                if (j >= this.maxSize) {
                    bits <<= 8L;
                    continue; // skip
                }
                // calculate voxel shape
                boolean[] bools = mapper.apply(j);
                // push all bools in the long (push from right to left)
                for (int k = bools.length - 1; k >= 0; k--) {
                    bits <<= 1L;
                    if (bools[k]) {
                        bits |= 1L;
                    }
                }
            }
            this.occlusionMappings[i >> 3] = bits;
        }
        this.built = true;
    }

    public boolean isBuilt() {
        return this.built;
    }
}
