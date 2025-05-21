package de.pianoman911.playerculling.platformcommon.platform.world;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PlatformChunkAccess {

    int getBlockId(int x, int y, int z);

    boolean isOpaque(int x, int y, int z, int voxelIndex);
}
