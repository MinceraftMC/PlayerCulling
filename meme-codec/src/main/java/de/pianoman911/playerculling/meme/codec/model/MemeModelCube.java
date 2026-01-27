package de.pianoman911.playerculling.meme.codec.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record MemeModelCube(
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ
) {
}
