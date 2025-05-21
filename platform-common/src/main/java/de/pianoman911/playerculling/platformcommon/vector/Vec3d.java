package de.pianoman911.playerculling.platformcommon.vector;

import de.pianoman911.playerculling.platformcommon.util.BlockFace;
import de.pianoman911.playerculling.platformcommon.util.NumberUtil;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * Own implementation of a 3d-vector with a lazy-calculated length.
 */
@NullMarked
public final class Vec3d implements Cloneable {

    private static final double EPSILON = 1e-6;

    public double x;
    public double y;
    public double z;

    public double len;
    private boolean dirtyLen;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dirtyLen = true;
    }

    public Vec3d() {
    }

    public static Vec3d intersectionPoint(Vec3d vec1, Vec3d dir1, Vec3d vec2, Vec3d dir2) {
        if (dir1.cross(dir2).length() < EPSILON) {
            return null;
        }

        double t1 = calcT(vec1, dir1, vec2);
        if (Double.isNaN(t1) && Double.isNaN(calcT(vec2, dir2, vec1))) { // Calc t2 only if t1 is NaN
            return null;
        }

        double x = vec1.getX() + dir1.getX() * t1;
        double y = vec1.getY() + dir1.getY() * t1;
        double z = vec1.getZ() + dir1.getZ() * t1;
        return new Vec3d(x, y, z);
    }

    private static double calcT(Vec3d vec1, Vec3d dir1, Vec3d vec2) {
        double t = Double.NaN;
        if (dir1.getX() != 0) {
            t = (vec2.getX() - vec1.getX()) / dir1.getX();
        } else if (dir1.getY() != 0) {
            t = (vec2.getY() - vec1.getY()) / dir1.getY();
        } else if (dir1.getZ() != 0) {
            t = (vec2.getZ() - vec1.getZ()) / dir1.getZ();
        }
        return t;
    }

    public static Vec3d intersectionPointWithArea(Vec3d vec, Vec3d dir, Vec3d area, Vec3d span1, Vec3d span2) {
        Vec3d areaNormal = span1.clone().cross(span2);
        double t = t(areaNormal, area, vec, dir);
        return new Vec3d(vec.getX() + dir.getX() * t, vec.getY() + dir.getY() * t, vec.getZ() + dir.getZ() * t);
    }

    private static double t(Vec3d areaNormal, Vec3d area, Vec3d vec, Vec3d dir) {
        return (areaNormal.getX() * (area.getX() - vec.getX()) + areaNormal.getY() * (area.getY() - vec.getY()) + areaNormal.getZ() * (area.getZ() - vec.getZ())) / (areaNormal.getX() * dir.getX() + areaNormal.getY() * dir.getY() + areaNormal.getZ() * dir.getZ());
    }

    public final Vec3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d set(Vec3d other) {
        this.x = other.getX();
        this.y = other.getY();
        this.z = other.getZ();
        this.dirtyLen = true;
        return this;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public final void setAdd(Vec3d position, double x, double y, double z) {
        this.x = position.x + x;
        this.y = position.y + y;
        this.z = position.z + z;
        this.dirtyLen = true;
    }

    public final Vec3d normalize() {
        double length = this.length();
        this.x /= length;
        this.y /= length;
        this.z /= length;
        this.len = 1;
        return this;
    }

    public final Vec3d div(Vec3d other) {
        this.x /= other.x;
        this.y /= other.y;
        this.z /= other.z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d add(Vec3d other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d sub(Vec3d other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d mul(Vec3d other) {
        this.x *= other.x;
        this.y *= other.y;
        this.z *= other.z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d mul(double other) {
        this.x *= other;
        this.y *= other;
        this.z *= other;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d div(double other) {
        this.x /= other;
        this.y /= other;
        this.z /= other;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d oneDivide() {
        this.x = 1d / x;
        this.y = 1d / y;
        this.z = 1d / z;
        this.dirtyLen = true;
        return this;
    }

    public final Vec3d zero() {
        this.x = 0d;
        this.y = 0d;
        this.z = 0d;
        this.len = 0d;
        this.dirtyLen = false;
        return this;
    }

    public Vec3d floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        this.z = Math.floor(this.z);
        this.dirtyLen = true;
        return this;
    }

    private void recalculateLength() {
        this.len = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public final double length() {
        if (this.dirtyLen) {
            this.recalculateLength();
            this.dirtyLen = false;
        }
        return this.len;
    }

    public final double angle(Vec3d other) {
        return Math.acos(this.dot(other) / (this.length() * other.length()));
    }

    public final double dot(Vec3d other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public final Vec3d cross(Vec3d other) {
        double x = this.y * other.z - this.z * other.y;
        double y = this.z * other.x - this.x * other.z;
        double z = this.x * other.y - this.y * other.x;
        return this.set(x, y, z);
    }

    public final Vec3d rotate(Vec3d axis, double angle) {
        double sinAngle = Math.sin(-angle);
        double cosAngle = Math.cos(-angle);
        return this.cross(axis.mul(sinAngle)) // rotation on local X
                .add((this.mul(cosAngle)) // rotation on local Z
                        .add(axis.mul(this.dot(axis.mul(1 - cosAngle))))); // rotation on local Y
    }

    public double distanceSquared(Vec3d other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceSquared(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distance(Vec3d other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public final boolean isZero() {
        return this.x == 0 && this.y == 0 && this.z == 0;
    }

    public final double getX() {
        return this.x;
    }

    public final int getFloorX() {
        return NumberUtil.floor(this.x);
    }

    public final double getY() {
        return this.y;
    }

    public int getFloorY() {
        return NumberUtil.floor(this.y);
    }

    public final double getZ() {
        return this.z;
    }

    public final int getFloorZ() {
        return NumberUtil.floor(this.z);
    }

    public Vec3d relative(BlockFace face) {
        return this.relative(face, 1);
    }

    public Vec3d relative(BlockFace face, double distance) {
        return this.add(face.modX * distance, face.modY * distance, face.modZ * distance);
    }

    public final Vec3d cloneRelative(BlockFace face, double distance) {
        return this.clone().add(face.modX * distance, face.modY * distance, face.modZ * distance);
    }

    public Vec3d copyRelative(BlockFace face) {
        return this.cloneRelative(face, 1);
    }

    public Vec3i toVec3iFloored() {
        return this.toVec3iFloored(new Vec3i());
    }

    public Vec3i toVec3iFloored(Vec3i vec3i) {
        return vec3i.set(this.getFloorX(), this.getFloorY(), this.getFloorZ());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec3d other) {
            return this.x == other.x && this.y == other.y && this.z == other.z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }

    @Override
    public Vec3d clone() {
        try {
            return (Vec3d) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }
}
