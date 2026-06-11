package de.pianoman911.playerculling.platformcommon.internals;

public interface ChunkCacheInterface {

    boolean[] isOpaqueFullBlock(int x, int y, int z);

    boolean isOccluded(double x, double y, double z);
}
