package de.pianoman911.playerculling.meme.data;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CubeDeformation(
        float growX,
        float growY,
        float growZ
) {

    @Override
    public String toString() {
        return "CubeDeformation{" +
                "growX=" + growX +
                ", growY=" + growY +
                ", growZ=" + growZ +
                '}';
    }
}
