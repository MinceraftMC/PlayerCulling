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
        drawBoundingBox(entity.getWorld(), entity.getBoundingBox(), color);
    }

    public static void drawBoundingBox(PlatformWorld world, AABB box) {
        drawBoundingBox(world, box, Color.BLUE);
    }

    public static void drawBoundingBox(PlatformWorld world, AABB box, Color color) {
        // minX -> maxX
        drawLine(world, new Vec3d(box.minX(), box.minY(), box.minZ()), new Vec3d(box.maxX(), box.minY(), box.minZ()), color);
        drawLine(world, new Vec3d(box.minX(), box.minY(), box.maxZ()), new Vec3d(box.maxX(), box.minY(), box.maxZ()), color);
        drawLine(world, new Vec3d(box.minX(), box.maxY(), box.minZ()), new Vec3d(box.maxX(), box.maxY(), box.minZ()), color);
        drawLine(world, new Vec3d(box.minX(), box.maxY(), box.maxZ()), new Vec3d(box.maxX(), box.maxY(), box.maxZ()), color);

        // minY -> maxY
        drawLine(world, new Vec3d(box.minX(), box.minY(), box.minZ()), new Vec3d(box.minX(), box.maxY(), box.minZ()), color);
        drawLine(world, new Vec3d(box.maxX(), box.minY(), box.minZ()), new Vec3d(box.maxX(), box.maxY(), box.minZ()), color);
        drawLine(world, new Vec3d(box.minX(), box.minY(), box.maxZ()), new Vec3d(box.minX(), box.maxY(), box.maxZ()), color);
        drawLine(world, new Vec3d(box.maxX(), box.minY(), box.maxZ()), new Vec3d(box.maxX(), box.maxY(), box.maxZ()), color);

        // minZ -> maxZ
        drawLine(world, new Vec3d(box.minX(), box.minY(), box.minZ()), new Vec3d(box.minX(), box.minY(), box.maxZ()), color);
        drawLine(world, new Vec3d(box.maxX(), box.minY(), box.minZ()), new Vec3d(box.maxX(), box.minY(), box.maxZ()), color);
        drawLine(world, new Vec3d(box.minX(), box.maxY(), box.minZ()), new Vec3d(box.minX(), box.maxY(), box.maxZ()), color);
        drawLine(world, new Vec3d(box.maxX(), box.maxY(), box.minZ()), new Vec3d(box.maxX(), box.maxY(), box.maxZ()), color);
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
