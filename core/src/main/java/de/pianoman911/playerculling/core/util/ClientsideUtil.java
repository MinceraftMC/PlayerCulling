package de.pianoman911.playerculling.core.util;


import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;

public final class ClientsideUtil {

    public static final float EPSILON = 1.0E-6F; // Mojang's epsilon value from com.mojang.math.Constants.EPSILON

    // Adds the player's view offset to the given vector (for F5 mode) only if the player is in third person mode
    public static void addPlayerViewOffset(Vec3d vec, PlatformPlayer player, CameraMode mode, double modifier) {
        double yaw = player.getYaw();
        double pitch = player.getPitch();

        if (mode == CameraMode.THIRD_PERSON_BACK) {
            yaw += 180;
            pitch *= -1;
        }

        yaw = Math.toRadians(yaw);
        pitch = Math.toRadians(pitch);

        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);

        vec.x += x * modifier;
        vec.y += y * modifier;
        vec.z += z * modifier;
    }

    public static void addPlayerViewOffset(Vec3d vec, PlatformPlayer player, CameraMode mode) {
        addPlayerViewOffset(vec, player, mode, maxZoom(player, mode));
    }

    private static double maxZoom(PlatformPlayer player, CameraMode mode) {
        double value = 4;
        Vec3d pos = player.getEyePosition();
        Vec3d dir = player.getDirection();

        Vec3d ray = switch (mode) {
            case THIRD_PERSON_BACK -> player.getWorld().rayTraceBlocks(pos, dir.mul(-1), 4);
            case THIRD_PERSON_FRONT -> player.getWorld().rayTraceBlocks(pos, dir, 4);
            case FIRST_PERSON -> null;
        };
        if (ray != null) {
            value = ray.distance(pos) - EPSILON;
        }

        return value;
    }
}
