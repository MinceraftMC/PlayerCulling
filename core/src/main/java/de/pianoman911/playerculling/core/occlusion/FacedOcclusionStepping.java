package de.pianoman911.playerculling.core.occlusion;


import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.util.BlockFace;
import de.pianoman911.playerculling.platformcommon.util.NumberUtil;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;

/**
 * This class is used to step through the voxels in a 3D grid using a faced occlusion algorithm.
 * Highly optimized for performance.
 * Completely works on the stack.
 */
public final class FacedOcclusionStepping {

    private static final int GRID_SIZE = 1 << 24;

    private static double getPos(double dir, double pos, double step) {
        return dir > 0 ? (pos - step) : (step + 1 - pos);
    }

    /**
     * The holy grail of allocationless voxel stepping. This method is called very often.
     * It is highly optimized for performance and should not be changed unless you know what you are doing.
     * This is on purpose a very long method, because everything is inlined and works on the stack.
     * <p>
     * "This is right here is the most important question of programming: Is this too much voodoo?" - Terry A. Davis
     *
     * @return positive ray steps if occluded, negative ray steps if not occluded
     */
    public static int scanOccluded(
            DataProvider provider, Vec3d start, Vec3i startVoxel,
            double maxDistanceSqrt, double dirX, double dirY, double dirZ
    ) {
        final int maxDistanceInt;

        long secondError;
        final long secondStep;
        long thirdError;
        final long thirdStep;

        final int mainX;
        final int mainY;
        final int mainZ;

        final int mainSecondThirdX;
        final int mainSecondThirdY;
        final int mainSecondThirdZ;

        final int mainSecondX;
        final int mainSecondY;
        final int mainSecondZ;

        final int mainThirdX;
        final int mainThirdY;
        final int mainThirdZ;

        int x0;
        int y0;
        int z0;

        { // Reduce locals in the scope
            double mainDirection;
            final double secondDirection;
            final double thirdDirection;

            final double mainPosition;
            final double secondPosition;
            final double thirdPosition;

            final BlockFace mainFace;
            final BlockFace secondFace;
            final BlockFace thirdFace;

            mainDirection = Math.abs(dirX);
            if (Math.abs(dirY) > mainDirection) {
                mainDirection = Math.abs(dirY);
                if (Math.abs(dirZ) > mainDirection) {
                    mainDirection = Math.abs(dirZ);
                    mainFace = dirZ > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
                    mainPosition = getPos(dirZ, start.getZ(), startVoxel.getZ());

                    secondDirection = Math.abs(dirX);
                    secondFace = dirX > 0 ? BlockFace.EAST : BlockFace.WEST;
                    secondPosition = getPos(dirX, start.getX(), startVoxel.getX());

                    thirdDirection = Math.abs(dirY);
                    thirdFace = dirY > 0 ? BlockFace.UP : BlockFace.DOWN;
                    thirdPosition = getPos(dirY, start.getY(), startVoxel.getY());
                } else {
                    mainFace = dirY > 0 ? BlockFace.UP : BlockFace.DOWN;
                    mainPosition = getPos(dirY, start.getY(), startVoxel.getY());

                    secondDirection = Math.abs(dirZ);
                    secondFace = dirZ > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
                    secondPosition = getPos(dirZ, start.getZ(), startVoxel.getZ());

                    thirdDirection = Math.abs(dirX);
                    thirdFace = dirX > 0 ? BlockFace.EAST : BlockFace.WEST;
                    thirdPosition = getPos(dirX, start.getX(), startVoxel.getX());
                }
            } else {
                if (Math.abs(dirZ) > mainDirection) {
                    mainDirection = Math.abs(dirZ);
                    mainFace = dirZ > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
                    mainPosition = getPos(dirZ, start.getZ(), startVoxel.getZ());

                    secondDirection = Math.abs(dirX);
                    secondFace = dirX > 0 ? BlockFace.EAST : BlockFace.WEST;
                    secondPosition = getPos(dirX, start.getX(), startVoxel.getX());

                    thirdDirection = Math.abs(dirY);
                    thirdFace = dirY > 0 ? BlockFace.UP : BlockFace.DOWN;
                    thirdPosition = getPos(dirY, start.getY(), startVoxel.getY());
                } else {
                    mainFace = dirX > 0 ? BlockFace.EAST : BlockFace.WEST;
                    mainPosition = getPos(dirX, start.getX(), startVoxel.getX());

                    secondDirection = Math.abs(dirY);
                    secondFace = dirY > 0 ? BlockFace.UP : BlockFace.DOWN;
                    secondPosition = getPos(dirY, start.getY(), startVoxel.getY());

                    thirdDirection = Math.abs(dirZ);
                    thirdFace = dirZ > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
                    thirdPosition = getPos(dirZ, start.getZ(), startVoxel.getZ());
                }
            }

            final double distance = mainPosition / mainDirection; // Distance to first intersection
            secondError = NumberUtil.floor((secondPosition - secondDirection * distance) * GRID_SIZE); // Error of second intersection (in grid units)
            secondStep = NumberUtil.floor(secondDirection / mainDirection * GRID_SIZE); // Step size to second intersection (in grid units)
            thirdError = NumberUtil.floor((thirdPosition - thirdDirection * distance) * GRID_SIZE); // Error of third intersection (in grid units)
            thirdStep = NumberUtil.floor(thirdDirection / mainDirection * GRID_SIZE); // Step size to third intersection (in grid units)

            if (secondError + secondStep <= 0) { // If second intersection is behind the first intersection (or on it)
                secondError = -secondStep + 1; // Step to second intersection
            }

            if (thirdError + thirdStep <= 0) { // If third intersection is behind the first intersection (or on it)
                thirdError = -thirdStep + 1; // Step to third intersection
            }


            // Last voxel before first intersection
            x0 = startVoxel.getX() - mainFace.modX;
            y0 = startVoxel.getY() - mainFace.modY;
            z0 = startVoxel.getZ() - mainFace.modZ;

            if (secondError < 0) { // If second intersection is behind the first intersection (or on it)
                secondError += GRID_SIZE;
                x0 -= secondFace.modX;
                y0 -= secondFace.modY;
                z0 -= secondFace.modZ;
            }

            if (thirdError < 0) { // If third intersection is behind the first intersection (or on it)
                thirdError += GRID_SIZE;
                x0 -= thirdFace.modX;
                y0 -= thirdFace.modY;
                z0 -= thirdFace.modZ;
            }

            secondError -= GRID_SIZE; // If positive, this is the error of the second intersection (in grid units)
            thirdError -= GRID_SIZE; // If positive, this is the error of the third intersection (in grid units)

            // Calculate the maximum distance
            {
                final double dirLenSqrt = mainDirection * mainDirection + secondDirection * secondDirection + thirdDirection * thirdDirection;
                maxDistanceInt = NumberUtil.roundPositive(Math.sqrt(maxDistanceSqrt / dirLenSqrt) * mainDirection);
            }

            // Vector of the main direction
            mainX = mainFace.modX;
            mainY = mainFace.modY;
            mainZ = mainFace.modZ;

            // Vector of the main direction + secondary direction + tertiary direction
            mainSecondThirdX = mainX + secondFace.modX + thirdFace.modX;
            mainSecondThirdY = mainY + secondFace.modY + thirdFace.modY;
            mainSecondThirdZ = mainZ + secondFace.modZ + thirdFace.modZ;

            // Vector of the main direction + secondary direction
            mainSecondX = mainX + secondFace.modX;
            mainSecondY = mainY + secondFace.modY;
            mainSecondZ = mainZ + secondFace.modZ;

            // Vector of the main direction + tertiary direction
            mainThirdX = mainX + thirdFace.modX;
            mainThirdY = mainY + thirdFace.modY;
            mainThirdZ = mainZ + thirdFace.modZ;
        }
        // Check vector
        int xC;
        int yC;
        int zC;

        int currentDistance = 0; // Ray steps

        while (currentDistance <= maxDistanceInt) {
            // Step forward
            currentDistance++; // main
            secondError += secondStep; // secondary
            thirdError += thirdStep; // tertiary

            xC = x0 + mainX;
            yC = y0 + mainY;
            zC = z0 + mainZ;

            if (provider.isOpaqueFullCube(xC, yC, zC)) { // If the voxel is opaque break
                return currentDistance;
            } else if (secondError > 0 && thirdError > 0) { // If we are at the second and third intersection
                x0 += mainSecondThirdX;
                y0 += mainSecondThirdY;
                z0 += mainSecondThirdZ;

                // Reset errors
                thirdError -= GRID_SIZE;
                secondError -= GRID_SIZE;
            } else if (secondError > 0) { // If we are at the second intersection
                x0 += mainSecondX;
                y0 += mainSecondY;
                z0 += mainSecondZ;

                // Reset error
                secondError -= GRID_SIZE;
            } else if (thirdError > 0) { // If we are at the third intersection
                x0 += mainThirdX;
                y0 += mainThirdY;
                z0 += mainThirdZ;

                // Reset error
                thirdError -= GRID_SIZE;
            } else {
                x0 = xC;
                y0 = yC;
                z0 = zC;
            }
        }
        return -currentDistance;
    }
}
