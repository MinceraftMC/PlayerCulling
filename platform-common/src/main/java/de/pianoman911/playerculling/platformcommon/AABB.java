package de.pianoman911.playerculling.platformcommon;

import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AABB {

    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABB() {
    }

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

    public AABB scale(double scale) {
        this.minX *= scale;
        this.minY *= scale;
        this.minZ *= scale;
        this.maxX *= scale;
        this.maxY *= scale;
        this.maxZ *= scale;
        return this;
    }

    public AABB scaledCopy(double scale) {
        return new AABB(
                this.minX * scale,
                this.minY * scale,
                this.minZ * scale,
                this.maxX * scale,
                this.maxY * scale,
                this.maxZ * scale
        );
    }

    public double getMinX() {
        return this.minX;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public double getMinY() {
        return this.minY;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public double getMinZ() {
        return this.minZ;
    }

    public void setMinZ(double minZ) {
        this.minZ = minZ;
    }

    public double getMaxX() {
        return this.maxX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public double getMaxY() {
        return this.maxY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public double getMaxZ() {
        return this.maxZ;
    }

    public void setMaxZ(double maxZ) {
        this.maxZ = maxZ;
    }
}
