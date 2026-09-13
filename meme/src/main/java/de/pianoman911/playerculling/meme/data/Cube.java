package de.pianoman911.playerculling.meme.data;

import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record Cube(
        @Nullable String comment,
        Vector3fc origin,
        Vector3fc dimensions,
        CubeDeformation grow,
        boolean mirror
        ) {

        @Override
        public String toString() {
                return "Cube{" +
                        "comment='" + comment + '\'' +
                        ", origin=" + origin +
                        ", dimensions=" + dimensions +
                        ", grow=" + grow +
                        ", mirror=" + mirror +
                        '}';
        }
}
