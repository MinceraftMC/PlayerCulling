package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativeLibLoader;
import de.pianoman911.playerculling.natives.NativesAdapter;
import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.occlusion.OcclusionCullingInterface;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.function.Function;

public final class Avx2Bridge implements NativesAdapter {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle CREATE_CHUNK_CACHE;
    private static final MethodHandle CREATE_OCCLUSION_INSTANCE;

    static {
        NativeLibLoader.loadLib("playerculling_avx2");

        LOOKUP = SymbolLookup.loaderLookup();

        CREATE_CHUNK_CACHE = LINKER.downcallHandle(
                LOOKUP.findOrThrow("create_chunk_cache"),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
        );

        CREATE_OCCLUSION_INSTANCE = LINKER.downcallHandle(
                LOOKUP.findOrThrow("create_occlusion_instance"),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
    }


    public static Linker linker() {
        return LINKER;
    }

    public static SymbolLookup lookup() {
        return LOOKUP;
    }

    public static ChunkCache createChunkCache() {
        try {
            MemorySegment pointer = (MemorySegment) CREATE_CHUNK_CACHE.invokeExact();
            return new ChunkCache(pointer);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    public static OcclusionInstance createOcclusionInstance() {
        try {
            MemorySegment pointer = (MemorySegment) CREATE_OCCLUSION_INSTANCE.invokeExact();
            return new OcclusionInstance(pointer);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Function<DataProvider, OcclusionCullingInterface> providerCullingInterface() {
        return _ -> createOcclusionInstance();
    }
}
