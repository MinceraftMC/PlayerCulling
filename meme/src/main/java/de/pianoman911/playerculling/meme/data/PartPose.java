package de.pianoman911.playerculling.meme.data;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale,
                       float zScale) {

    @Override
    public String toString() {
        return "PartPose{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", xRot=" + xRot +
                ", yRot=" + yRot +
                ", zRot=" + zRot +
                ", xScale=" + xScale +
                ", yScale=" + yScale +
                ", zScale=" + zScale +
                '}';
    }
}
