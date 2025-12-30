package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface SingleEntityResolver extends ArgumentResolver<PlatformEntity> {
}
