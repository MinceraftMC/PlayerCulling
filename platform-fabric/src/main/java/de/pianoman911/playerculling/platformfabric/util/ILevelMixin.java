package de.pianoman911.playerculling.platformfabric.util;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;


public interface ILevelMixin {

    @Unique
    Set<BlockPos> getChangedBlocks();
}
