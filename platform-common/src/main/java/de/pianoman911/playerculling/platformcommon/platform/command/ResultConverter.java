package de.pianoman911.playerculling.platformcommon.platform.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface ResultConverter<T, R> {

    R convert(T type) throws CommandSyntaxException;
}
