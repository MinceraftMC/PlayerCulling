package de.pianoman911.playerculling.platformfabric1217.common;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public interface ILevel {

    Set<BlockPos> getChangedBlocks();
}
