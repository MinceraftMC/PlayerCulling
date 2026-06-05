package de.pianoman911.playerculling.natives;

import java.lang.foreign.MemorySegment;

public abstract class NativePart {

    protected MemorySegment pointer;

    public NativePart(MemorySegment pointer) {
        this.pointer = pointer;
    }

    public MemorySegment getPointer() {
        return this.pointer;
    }
}
