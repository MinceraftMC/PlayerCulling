package de.pianoman911.playerculling.platformcommon.occlusion;

import de.pianoman911.playerculling.platformcommon.vector.Vec3d;

public interface OcclusionCullingInterface {

    long getAndResetRaySteps();

    boolean isAABBVisible(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            Vec3d viewerPosition
    );
}
