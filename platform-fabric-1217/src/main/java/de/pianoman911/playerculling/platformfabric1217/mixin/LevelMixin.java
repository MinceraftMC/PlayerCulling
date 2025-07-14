package de.pianoman911.playerculling.platformfabric1217.mixin;

import de.pianoman911.playerculling.platformfabric1217.common.ILevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@NullMarked
@Mixin(Level.class)
@Implements(@Interface(iface = ILevel.class, prefix = "playerculling$"))
public abstract class LevelMixin implements ILevel {

    @Unique
    private final Set<BlockPos> changedBlocks = new HashSet<>();

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN")
    )
    public void setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValueZ()) {
            this.changedBlocks.add(pos);
        }
    }

    public Set<BlockPos> playerculling$getChangedBlocks() {
        return this.changedBlocks;
    }
}
