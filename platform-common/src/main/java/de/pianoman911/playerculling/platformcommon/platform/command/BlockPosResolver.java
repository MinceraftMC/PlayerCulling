package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface BlockPosResolver extends ArgumentResolver<Vec3i> {
}
