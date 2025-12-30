package de.pianoman911.playerculling.platformcommon.util;

import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jspecify.annotations.NullMarked;

import java.awt.Color;

@NullMarked
public final class DebugUtil {

    private static final double SPACE = 0.1d;

    private DebugUtil() {
    }

    public static void drawBoundingBox(PlatformEntity entity) {
        drawBoundingBox(entity, Color.GREEN);
    }

    public static void drawBoundingBox(PlatformEntity entity, Color color) {
        drawBoundingBox(entity.getWorld(), entity.getScaledBoundingBox().scaledCopy(0.5d), color);
    }

    public static void drawBoundingBox(PlatformWorld world, AABB box) {
        drawBoundingBox(world, box, Color.BLUE);
    }

    public static void drawBoundingBox(PlatformWorld world, AABB box, Color color) {
        // getMinX -> getMaxX
        drawLine(world, new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()), new Vec3d(box.getMaxX(), box.getMinY(), box.getMinZ()), color);
        drawLine(world, new Vec3d(box.getMinX(), box.getMinY(), box.getMaxZ()), new Vec3d(box.getMaxX(), box.getMinY(), box.getMaxZ()), color);
        drawLine(world, new Vec3d(box.getMinX(), box.getMaxY(), box.getMinZ()), new Vec3d(box.getMaxX(), box.getMaxY(), box.getMinZ()), color);
        drawLine(world, new Vec3d(box.getMinX(), box.getMaxY(), box.getMaxZ()), new Vec3d(box.getMaxX(), box.getMaxY(), box.getMaxZ()), color);

        // getMinY -> getMaxY
        drawLine(world, new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()), new Vec3d(box.getMinX(), box.getMaxY(), box.getMinZ()), color);
        drawLine(world, new Vec3d(box.getMaxX(), box.getMinY(), box.getMinZ()), new Vec3d(box.getMaxX(), box.getMaxY(), box.getMinZ()), color);
        drawLine(world, new Vec3d(box.getMinX(), box.getMinY(), box.getMaxZ()), new Vec3d(box.getMinX(), box.getMaxY(), box.getMaxZ()), color);
        drawLine(world, new Vec3d(box.getMaxX(), box.getMinY(), box.getMaxZ()), new Vec3d(box.getMaxX(), box.getMaxY(), box.getMaxZ()), color);

        // getMinZ -> getMaxZ
        drawLine(world, new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()), new Vec3d(box.getMinX(), box.getMinY(), box.getMaxZ()), color);
        drawLine(world, new Vec3d(box.getMaxX(), box.getMinY(), box.getMinZ()), new Vec3d(box.getMaxX(), box.getMinY(), box.getMaxZ()), color);
        drawLine(world, new Vec3d(box.getMinX(), box.getMaxY(), box.getMinZ()), new Vec3d(box.getMinX(), box.getMaxY(), box.getMaxZ()), color);
        drawLine(world, new Vec3d(box.getMaxX(), box.getMaxY(), box.getMinZ()), new Vec3d(box.getMaxX(), box.getMaxY(), box.getMaxZ()), color);
    }

    public static void drawLine(PlatformWorld world, Vec3d point1, Vec3d point2, Color color) {
        Vec3d direction = point2.clone().sub(point1).normalize().mul(SPACE);
        double distance = point1.distance(point2);

        Vec3d currentPoint = point1.clone();
        for (double i = 0; i < distance; i += SPACE) {
            currentPoint.add(direction);

            world.spawnColoredParticle(
                    currentPoint.getX(),
                    currentPoint.getY(),
                    currentPoint.getZ(),
                    color,
                    0.5f
            );
        }
    }
}
