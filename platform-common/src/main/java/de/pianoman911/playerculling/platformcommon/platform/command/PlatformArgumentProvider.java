package de.pianoman911.playerculling.platformcommon.platform.command;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PlatformArgumentProvider {

    ArgumentType<BlockPosResolver> blockPos();

    ArgumentType<SinglePlayerResolver> player();

    ArgumentType<MultiPlayerResolver> players();

    default ArgumentType<?> mapFromNms(ArgumentType<?> instance) {
        return instance;
    }

    default ArgumentType<?> mapToNms(ArgumentType<?> instance) {
        return instance;
    }
}
