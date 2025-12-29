package de.pianoman911.playerculling.core.occlusion;

import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 Copied from https://github.com/LogisticsCraft/OcclusionCulling
 commit: 3a813013e84a07e6968c47b63959ff92f050d19f
 license: MIT

 changed from standard ray casting to faced approach -> more reliable and faster (pianoman911)
 changed from block based to voxel based (2x2x2 Voxels per block) (pianoman911)
 changed from using a boolean array to using a single short for target point selection (booky)
 also inlined various methods and reduced more allocations (booky)
 changed from heap usage to stack usage (pianoman911, booky)
 */

public final class OcclusionCullingInstance {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");

    private static final double SAFE_POINT_OFFSET = 0.05d;
    private static final double TWO_SAFE_POINT_OFFSET = SAFE_POINT_OFFSET * 2d;
    private static final double POINT_START = SAFE_POINT_OFFSET;
    private static final double POINT_END = 1d - SAFE_POINT_OFFSET;
    private static final double POINT_MIDDLE = 1d / 2d;

    private static final double DELTA = 1;
    private static final byte ON_MIN_X = 1;
    private static final byte ON_MAX_X = 1 << 1;
    private static final byte ON_MIN_Y = 1 << 2;
    private static final byte ON_MAX_Y = 1 << 3;
    private static final byte ON_MIN_Z = 1 << 4;
    private static final byte ON_MAX_Z = 1 << 5;

    private final DataProvider provider;

    // Reused allocated data structures
    private final Vec3i startVoxel = new Vec3i(0, 0, 0);
    private long raySteps;

    public OcclusionCullingInstance(DataProvider provider) {
        this.provider = provider;
    }

    private static boolean deltaLowerThanX(double a, double b) {
        return Math.abs(a - b) < DELTA;
    }

    private static double safePointEndOffset(double target, double max) {
        return Math.min(target + POINT_END, max - SAFE_POINT_OFFSET);
    }

    private static double safeMiddleOffset(double target, double max) {
        if (target + POINT_MIDDLE < max - SAFE_POINT_OFFSET) {
            return target + POINT_MIDDLE;
        }
        // choose point in middle between target and max
        return target + SAFE_POINT_OFFSET + ((max - SAFE_POINT_OFFSET) - (target + SAFE_POINT_OFFSET)) * POINT_MIDDLE;
    }

    public boolean isAABBVisible(Vec3d aabbMin, Vec3d aabbMax, Vec3d viewerPosition) {
        return this.isAABBVisible(
                aabbMin.x, aabbMin.y, aabbMin.z,
                aabbMax.x, aabbMax.y, aabbMax.z,
                viewerPosition
        );
    }

    @SuppressWarnings({"DuplicatedCode", "SuspiciousNameCombination"})
    public boolean isAABBVisible(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            Vec3d viewerPosition
    ) {
        try {
            Vec3i startVoxel = viewerPosition.toVec3iFloored(this.startVoxel);

            Relative relX = Relative.from(minX, maxX, viewerPosition.x);
            Relative relY = Relative.from(minY, maxY, viewerPosition.y);
            Relative relZ = Relative.from(minZ, maxZ, viewerPosition.z);

            if (relX == Relative.INSIDE && relY == Relative.INSIDE && relZ == Relative.INSIDE) {
                return true; // We are inside the AABB, don't cull
            }
            // We are outside the AABB -> Go for culling

            // Loop for voxel positions -> only check the faces that have faces to the outside
            for (double x = minX; x < maxX - TWO_SAFE_POINT_OFFSET; x++) {
                byte visibleOnFaceX = 0; // visible faces on the x-axis
                byte faceEdgeDataX = 0; // visible corners on the  x-axis

                // Only check the faces that are outside the AABB
                faceEdgeDataX |= (deltaLowerThanX(x, minX)) ? ON_MIN_X : 0;
                faceEdgeDataX |= (deltaLowerThanX(x, maxX)) ? ON_MAX_X : 0;

                // Only check the faces that are outside the AABB
                visibleOnFaceX |= (deltaLowerThanX(x, minX) && relX == Relative.POSITIVE) ? ON_MIN_X : 0;
                visibleOnFaceX |= (deltaLowerThanX(x, maxX) && relX == Relative.NEGATIVE) ? ON_MAX_X : 0;

                // Same for Y and Z
                for (double y = minY; y < maxY - TWO_SAFE_POINT_OFFSET; y++) {
                    byte faceEdgeDataY = faceEdgeDataX;
                    byte visibleOnFaceY = visibleOnFaceX;

                    faceEdgeDataY |= (deltaLowerThanX(y, minY)) ? ON_MIN_Y : 0;
                    faceEdgeDataY |= (deltaLowerThanX(y, maxY)) ? ON_MAX_Y : 0;

                    visibleOnFaceY |= (deltaLowerThanX(y, minY) && relY == Relative.POSITIVE) ? ON_MIN_Y : 0;
                    visibleOnFaceY |= (deltaLowerThanX(y, maxY) && relY == Relative.NEGATIVE) ? ON_MAX_Y : 0;

                    for (double z = minZ; z < maxZ - TWO_SAFE_POINT_OFFSET; z++) {
                        byte faceEdgeData = faceEdgeDataY;
                        byte visibleOnFace = visibleOnFaceY;

                        faceEdgeData |= (deltaLowerThanX(z, minZ)) ? ON_MIN_Z : 0;
                        faceEdgeData |= (deltaLowerThanX(z, maxZ)) ? ON_MAX_Z : 0;

                        visibleOnFace |= (deltaLowerThanX(z, minZ) && relZ == Relative.POSITIVE) ? ON_MIN_Z : 0;
                        visibleOnFace |= (deltaLowerThanX(z, maxZ) && relZ == Relative.NEGATIVE) ? ON_MAX_Z : 0;

                        if (visibleOnFace != 0) {
                            if (this.isVoxelVisible(viewerPosition, startVoxel, x, y, z, faceEdgeData, visibleOnFace,
                                    maxX, maxY, maxZ)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Throwable throwable) {
            LOGGER.error("Error while culling", throwable);
        }
        return true;
    }

    /**
     * Checks if a voxel is visible from a given position and face data.
     * It selects the visible points on the voxel and checks if they are visible.
     *
     * @param posStart      starting position of the ray
     * @param faceData      contains rather this Block is on the outside for a given face
     * @param visibleOnFace contains rather a face should be considered
     * @return true if the voxel is visible, false otherwise
     */
    private boolean isVoxelVisible(
            Vec3d posStart, Vec3i startVoxel, double targetX, double targetY, double targetZ,
            byte faceData, byte visibleOnFace, double maxX, double maxY, double maxZ
    ) {
        short dotselectors = 0; // 8 corners + 6 middle faces -> Cuboid, 14 bools

        // Select faces and corners that are visible of the voxel
        if ((visibleOnFace & ON_MIN_X) == ON_MIN_X) {
            dotselectors |= (1 << 0) | (1 << 8);
            if ((faceData & ~ON_MIN_X) != 0) {
                dotselectors |= (1 << 1) | (1 << 4) | (1 << 5);
            }
        }
        if ((visibleOnFace & ON_MIN_Y) == ON_MIN_Y) {
            dotselectors |= (1 << 0) | (1 << 9);
            if ((faceData & ~ON_MIN_Y) != 0) {
                dotselectors |= (1 << 3) | (1 << 4) | (1 << 7);
            }
        }
        if ((visibleOnFace & ON_MIN_Z) == ON_MIN_Z) {
            dotselectors |= (1 << 0) | (1 << 10);
            if ((faceData & ~ON_MIN_Z) != 0) {
                dotselectors |= (1 << 1) | (1 << 4) | (1 << 5);
            }
        }

        // we assume the target is always at least SAFE_POINT_OFFSET away from max, otherwise there
        // is not enough space to perform a raycast; this is guaranteed by our for-loop upper bound in #isAABBVisible
        double targetBeginX = targetX + POINT_START;
        double targetBeginY = targetY + POINT_START;
        double targetBeginZ = targetZ + POINT_START;

        // corners
        if ((dotselectors & (1 << 0)) == 1 << 0 // minX, minY, minZ
                && this.scanVisible(posStart, startVoxel, targetBeginX, targetBeginY, targetBeginZ)) {
            return true;
        }

        if ((visibleOnFace & ON_MAX_Y) == ON_MAX_Y) {
            dotselectors |= (1 << 1) | (1 << 12);
            if ((faceData & ~ON_MAX_Y) != 0) {
                dotselectors |= (1 << 2) | (1 << 5) | (1 << 6);
            }
        }
        double targetEndY = safePointEndOffset(targetY, maxY);
        if ((dotselectors & (1 << 1)) == 1 << 1 // minX, maxY, minZ
                && this.scanVisible(posStart, startVoxel, targetBeginX, targetEndY, targetBeginZ)) {
            return true;
        }

        if ((visibleOnFace & ON_MAX_Z) == ON_MAX_Z) {
            dotselectors |= (1 << 2) | (1 << 13);
            if ((faceData & ~ON_MAX_Z) != 0) {
                dotselectors |= (1 << 3) | (1 << 6) | (1 << 7);
            }
        }
        double targetEndZ = safePointEndOffset(targetZ, maxZ);
        if ((dotselectors & (1 << 2)) == 1 << 2 // minX, maxY, maxZ
                && this.scanVisible(posStart, startVoxel, targetBeginX, targetEndY, targetEndZ)) {
            return true;
        }
        if ((dotselectors & (1 << 3)) == 1 << 3 // minX, minY, maxZ
                && this.scanVisible(posStart, startVoxel, targetBeginX, targetBeginY, targetEndZ)) {
            return true;
        }

        if ((visibleOnFace & ON_MAX_X) == ON_MAX_X) {
            dotselectors |= (1 << 4) | (1 << 11);
            if ((faceData & ~ON_MAX_X) != 0) {
                dotselectors |= (1 << 5) | (1 << 6) | (1 << 7);
            }
        }
        double targetEndX = safePointEndOffset(targetX, maxX);
        if ((dotselectors & (1 << 4)) == 1 << 4 // maxX, minY, minZ
                && this.scanVisible(posStart, startVoxel, targetEndX, targetBeginY, targetBeginZ)) {
            return true;
        }
        if ((dotselectors & (1 << 5)) == 1 << 5 // maxX, maxY, minZ
                && this.scanVisible(posStart, startVoxel, targetEndX, targetEndY, targetBeginZ)) {
            return true;
        }
        if ((dotselectors & (1 << 6)) == 1 << 6 // maxX, maxY, maxZ
                && this.scanVisible(posStart, startVoxel, targetEndX, targetEndY, targetEndZ)) {
            return true;
        }
        if ((dotselectors & (1 << 7)) == 1 << 7 // maxX, minY, maxZ
                && this.scanVisible(posStart, startVoxel, targetEndX, targetBeginY, targetEndZ)) {
            return true;
        }

        // middle points
        double safeMiddleY = safeMiddleOffset(targetY, maxY);
        double safeMiddleZ = safeMiddleOffset(targetZ, maxZ);
        if ((dotselectors & (1 << 8)) == 1 << 8 // minX, 0.5, 0.5
                && this.scanVisible(posStart, startVoxel, targetBeginX, safeMiddleY, safeMiddleZ)) {
            return true;
        }
        double safeMiddleX = safeMiddleOffset(targetX, maxX);
        if ((dotselectors & (1 << 9)) == 1 << 9 // 0.5, minY, 0.5
                && this.scanVisible(posStart, startVoxel, safeMiddleX, targetBeginY, safeMiddleZ)) {
            return true;
        }
        if ((dotselectors & (1 << 10)) == 1 << 10 // 0.5, 0.5, minZ
                && this.scanVisible(posStart, startVoxel, safeMiddleX, safeMiddleY, targetBeginZ)) {
            return true;
        }

        if ((dotselectors & (1 << 11)) == 1 << 11 // maxX, 0.5, 0.5
                && this.scanVisible(posStart, startVoxel, targetEndX, safeMiddleY, safeMiddleZ)) {
            return true;
        }
        if ((dotselectors & (1 << 12)) == 1 << 12 // 0.5, maxY, 0.5
                && this.scanVisible(posStart, startVoxel, safeMiddleX, targetEndY, safeMiddleZ)) {
            return true;
        }
        return (dotselectors & (1 << 13)) == 1 << 13 // 0.5, 0.5, maxZ
                && this.scanVisible(posStart, startVoxel, safeMiddleX, safeMiddleY, targetEndZ);
    }

    private boolean scanVisible(Vec3d posStart, Vec3i startVoxel, double x, double y, double z) {
        double dirX = x - posStart.x;
        double dirY = y - posStart.y;
        double dirZ = z - posStart.z;
        double dirLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        int steps = FacedOcclusionStepping.scanOccluded(
                this.provider, posStart, startVoxel, posStart.distanceSquared(x, y, z),
                dirX / dirLen, dirY / dirLen, dirZ / dirLen);
        if (steps < 0) {
            this.raySteps -= steps;
            return true;
        }
        this.raySteps += steps;
        return false;
    }

    public long getAndResetRaySteps() {
        long steps = this.raySteps;
        this.raySteps = 0;
        return steps;
    }

    private enum Relative {
        INSIDE,
        POSITIVE,
        NEGATIVE;

        public static Relative from(double min, double max, double pos) {
            if (max > pos && min > pos) {
                return POSITIVE;
            } else if (min < pos && max < pos) {
                return NEGATIVE;
            }
            return INSIDE;
        }
    }
}
