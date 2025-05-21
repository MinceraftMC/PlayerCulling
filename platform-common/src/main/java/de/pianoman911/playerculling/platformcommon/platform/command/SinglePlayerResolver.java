package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface SinglePlayerResolver extends ArgumentResolver<PlatformPlayer> {
}
