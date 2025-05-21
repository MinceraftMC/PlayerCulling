package de.pianoman911.playerculling.platformcommon.platform.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface ArgumentResolver<T> {

    T resolve(PlatformCommandSourceStack sourceStack) throws CommandSyntaxException;
}
