package de.pianoman911.playerculling.platformcommon.vector;

import de.pianoman911.playerculling.platformcommon.util.BlockFace;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class Vec3i implements Cloneable {

    private int x;
    private int y;
    private int z;

    public Vec3i() {
    }

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public final Vec3i set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public final Vec3i add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public final Vec3i relative(BlockFace face) {
        return this.add(face.modX, face.modY, face.modZ);
    }

    public final int getX() {
        return this.x;
    }

    public final int getY() {
        return this.y;
    }

    public final int getZ() {
        return this.z;
    }

    public final boolean same(Vec3i vec) {
        return this.x == vec.x && this.y == vec.y && this.z == vec.z;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Vec3i vec3i = (Vec3i) object;
        return this.x == vec3i.x && this.y == vec3i.y && this.z == vec3i.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }

    @Override
    public Vec3i clone() {
        try {
            return (Vec3i) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }
}
