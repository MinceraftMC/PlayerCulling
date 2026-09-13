package de.pianoman911.playerculling.meme.data;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public record Model(
        List<Cube> cubes,
        PartPose partPose,
        Map<String, Model> children
) {

    @Override
    public String toString() {
        return "Model{" +
                "cubes=" + cubes +
                ", partPose=" + partPose +
                ", children=" + children +
                '}';
    }
}
