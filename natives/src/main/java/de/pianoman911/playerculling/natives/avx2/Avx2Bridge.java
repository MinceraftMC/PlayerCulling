package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativeLibLoader;
import de.pianoman911.playerculling.natives.NativesAdapter;
import de.pianoman911.playerculling.platformcommon.internals.OcclusionCullingInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class Avx2Bridge implements NativesAdapter {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final MethodHandle CREATE_CHUNK_CACHE;
    private static final MethodHandle CREATE_OCCLUSION_INSTANCE;
    private static final MethodHandle CREATE_DYNAMIC_WORLD;

    static {
        NativeLibLoader.loadLib("playerculling_avx2");

        LOOKUP = SymbolLookup.loaderLookup();

        CREATE_CHUNK_CACHE = LINKER.downcallHandle(
                LOOKUP.findOrThrow("create_world_cache"),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
        );

        CREATE_OCCLUSION_INSTANCE = LINKER.downcallHandle(
                LOOKUP.findOrThrow("create_occlusion_instance"),
                FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS // dynamic_world* dynamic_world
                )
        );

        CREATE_DYNAMIC_WORLD = LINKER.downcallHandle(
                LOOKUP.findOrThrow("create_dynamic_world"),
                FunctionDescriptor.of(ValueLayout.ADDRESS)
        );
    }


    public static Linker linker() {
        return LINKER;
    }

    public static SymbolLookup lookup() {
        return LOOKUP;
    }

    public static WorldCache createChunkCache(PlatformWorld<?> world) {
        try {
            MemorySegment pointer = (MemorySegment) CREATE_CHUNK_CACHE.invokeExact();
            return new WorldCache(pointer, world);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    public static OcclusionInstance createOcclusionInstance(PlatformPlayer player) {
        try {
            DynamicWorld dynamicWorld = createDynamicWorld();
            MemorySegment pointer = (MemorySegment) CREATE_OCCLUSION_INSTANCE.invokeExact(dynamicWorld.getPointer());
            return new OcclusionInstance(pointer, dynamicWorld, player);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    public static DynamicWorld createDynamicWorld() {
        try {
            MemorySegment pointer = (MemorySegment) CREATE_DYNAMIC_WORLD.invokeExact();
            return new DynamicWorld(pointer);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public OcclusionCullingInterface providerCullingInterface(PlatformPlayer player) {
        return createOcclusionInstance(player);
    }

    @Override
    public WorldCacheInterface providerWorldCache(PlatformWorld<?> world) {
        return createChunkCache(world);
    }
}
