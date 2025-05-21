package de.pianoman911.playerculling.platformfabric.util;

import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class BlockStateUtil {

    public static boolean[] buildVoxelShape(BlockState state, ServerLevel world) {
        if (state == null) {
            return OcclusionMappings.EMPTY_CUBE;
        }
        if (!state.canOcclude()) {
            return OcclusionMappings.EMPTY_CUBE;
        }
        if (state.isSolidRender()) {
            return OcclusionMappings.FULL_CUBE;
        }

        VoxelShape shape = state.getShape(world, BlockPos.ZERO);
        List<AABB> nms = shape.toAabbs();

        boolean[] shapes = new boolean[8];
        for (int i = 0; i < 8; i++) {
            double minX = (i & 1) * 0.5;
            double minY = ((i >> 1) & 1) * 0.5;
            double minZ = ((i >> 2) & 1) * 0.5;
            double maxX = minX + 0.5;
            double maxY = minY + 0.5;
            double maxZ = minZ + 0.5;

            boolean contains = false;
            for (AABB aabb : nms) { // Check if shape contains any of the 8 points completely
                if (boxContainsBox(aabb, minX, minY, minZ, maxX, maxY, maxZ)) {
                    contains = true;
                    break;
                }
            }
            shapes[i] = contains;
        }
        return shapes;
    }

    private static final boolean boxContainsBox(AABB outer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return outer.minX <= minX && outer.minY <= minY && outer.minZ <= minZ &&
                outer.maxX >= maxX && outer.maxY >= maxY && outer.maxZ >= maxZ;
    }
}
