package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativePart;

import java.lang.foreign.MemorySegment;

public class ChunkCache extends NativePart {

    public ChunkCache(MemorySegment pointer) {
        super(pointer);
    }
}
