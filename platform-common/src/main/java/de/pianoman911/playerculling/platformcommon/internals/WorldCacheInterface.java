package de.pianoman911.playerculling.platformcommon.internals;

public interface WorldCacheInterface {

    ChunkCacheInterface chunk(int cx, int cz);

    boolean hasChunk(int cx, int cz);

    void removeChunk(int cx, int cz);
}
