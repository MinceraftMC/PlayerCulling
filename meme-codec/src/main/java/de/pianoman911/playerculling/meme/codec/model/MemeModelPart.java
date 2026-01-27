package de.pianoman911.playerculling.meme.codec.model;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public record MemeModelPart(
    float x,
    float y,
    float z,
    float rotationX,
    float rotationY,
    float rotationZ,
    List<MemeModelCube> cubes,
    Map<String, MemeModelPart> children
) {

}
