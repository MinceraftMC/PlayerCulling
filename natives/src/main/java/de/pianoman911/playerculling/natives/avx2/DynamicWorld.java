package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativePart;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class DynamicWorld extends NativePart {

    private static final MethodHandle RESIZE;

    static {
        RESIZE = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_dynamic_world_resize"),
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, // dynamic_world* instance
                        ValueLayout.JAVA_INT // int32_t radius
                )
        );
    }

    public DynamicWorld(MemorySegment pointer) {
        super(pointer);
    }

    public void resize(int radius) {
        try {
            RESIZE.invokeExact(this.getPointer(), radius);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }
}
