package de.pianoman911.playerculling.meme.codec.model;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public record MemeModel(
        MemeModelMeta meta,
        Map<String, MemeModelPart> parts
) {
}
