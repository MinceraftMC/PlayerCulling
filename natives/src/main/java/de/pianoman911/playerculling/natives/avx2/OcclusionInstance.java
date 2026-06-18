package de.pianoman911.playerculling.natives.avx2;

import de.pianoman911.playerculling.natives.NativePart;
import de.pianoman911.playerculling.platformcommon.internals.DataProviderInterface;
import de.pianoman911.playerculling.platformcommon.internals.OcclusionCullingInterface;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class OcclusionInstance extends NativePart implements OcclusionCullingInterface {

    private static final MethodHandle IS_AABB_VISIBLE;
    private static final MethodHandle UPDATE_WORLD;

    static {
        IS_AABB_VISIBLE = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_occlusion_instance_is_aabb_visible"),
                FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
                        ValueLayout.ADDRESS, // occlusion_instance*
                        ValueLayout.JAVA_DOUBLE, // min_x
                        ValueLayout.JAVA_DOUBLE, // min_y
                        ValueLayout.JAVA_DOUBLE, // min_z
                        ValueLayout.JAVA_DOUBLE, // max_x
                        ValueLayout.JAVA_DOUBLE, // max_y
                        ValueLayout.JAVA_DOUBLE, // max_z
                        ValueLayout.JAVA_DOUBLE, // viewer_pos_x
                        ValueLayout.JAVA_DOUBLE, // viewer_pos_y
                        ValueLayout.JAVA_DOUBLE // viewer-pos_z
                )
        );
        UPDATE_WORLD = Avx2Bridge.linker().downcallHandle(
                Avx2Bridge.lookup().findOrThrow("cpp_occlusion_instance_update_world"),
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, // occlusion_instance*
                        ValueLayout.JAVA_INT, // int32_t ccx
                        ValueLayout.JAVA_INT, // int32_t ccz
                        ValueLayout.ADDRESS // WorldCache *world
                )
        );
    }

    private final DynamicWorld world;
    private final NativeDataProvider provider;

    public OcclusionInstance(MemorySegment pointer, DynamicWorld world,  PlatformPlayer player) {
        super(pointer);
        this.world = world;
        this.provider = new NativeDataProvider(player, this);
    }

    private boolean isAabbVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double viewerX, double viewerY, double viewerZ) {
        try {
            return (boolean) IS_AABB_VISIBLE.invokeExact(this.getPointer(), minX, minY, minZ, maxX, maxY, maxZ, viewerX, viewerY, viewerZ);
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    public void updateWorld(int ccx, int ccz, WorldCache worldCache) {
        try {
            UPDATE_WORLD.invokeExact(this.getPointer(), ccx, ccz, worldCache.getPointer());
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public long getAndResetRaySteps() {
        return 0;
    }

    @Override
    public boolean isAABBVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3d viewerPosition) {
        return this.isAabbVisible(minX, minY, minZ, maxX, maxY, maxZ, viewerPosition.x, viewerPosition.y, viewerPosition.z);
    }

    @Override
    public DataProviderInterface getDataProvider() {
        return this.provider;
    }

    public DynamicWorld getWorld() {
        return this.world;
    }
}
