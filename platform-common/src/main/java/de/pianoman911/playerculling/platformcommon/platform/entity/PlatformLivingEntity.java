package de.pianoman911.playerculling.platformcommon.platform.entity;

import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PlatformLivingEntity extends PlatformEntity {

    Vec3i getTargetBlock(int maxDistance);

    Vec3d getEyePosition();

    double getYaw();

    double getPitch();

    boolean isGlowing();
}
