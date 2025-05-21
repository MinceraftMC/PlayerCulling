package de.pianoman911.playerculling.platformcommon.platform.command;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface PlatformCommandSourceStack {

    PlatformCommandSender getSender();

    @Nullable
    PlatformEntity getExecutor();
}
