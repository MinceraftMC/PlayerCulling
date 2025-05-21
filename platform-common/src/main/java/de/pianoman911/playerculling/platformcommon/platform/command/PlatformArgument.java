package de.pianoman911.playerculling.platformcommon.platform.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PlatformArgument<I, P> {

    P convertToPlatform(I obj) throws CommandSyntaxException;
}
