package de.pianoman911.playerculling.platformcommon.platform.entity;

import de.pianoman911.playerculling.platformcommon.AABB;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface PlatformEntity extends PlatformCommandSender {

    int getEntityId();

    UUID getUniqueId();

    String getName();

    PlatformWorld getWorld();

    Vec3d getPosition();

    AABB getScaledBoundingBox();

    void teleport(PlatformWorld world, double x, double y, double z);
}
