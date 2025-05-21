package de.pianoman911.playerculling.platformcommon;

import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record AABB(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {

    public double getCenterX() {
        return this.minX + (this.maxX - this.minX) / 2;
    }

    public double getCenterY() {
        return this.minY + (this.maxY - this.minY) / 2;
    }

    public double getCenterZ() {
        return this.minZ + (this.maxZ - this.minZ) / 2;
    }

    public Vec3d getCenter() {
        return new Vec3d(this.getCenterX(), this.getCenterY(), this.getCenterZ());
    }

    public Vec3d getMin() {
        return new Vec3d(this.minX, this.minY, this.minZ);
    }

    public Vec3d getMax() {
        return new Vec3d(this.maxX, this.maxY, this.maxZ);
    }
}
