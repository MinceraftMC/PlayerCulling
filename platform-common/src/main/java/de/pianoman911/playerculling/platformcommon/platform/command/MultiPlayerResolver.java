package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

@NullMarked
@FunctionalInterface
public interface MultiPlayerResolver extends ArgumentResolver<Collection<PlatformPlayer>> {
}
