package de.pianoman911.playerculling.platformfabric1214.mixin;

import de.pianoman911.playerculling.platformfabric1214.util.ILevelMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(Level.class)
@Implements(@Interface(iface = ILevelMixin.class, prefix = "playerculling$"))
public abstract class LevelMixin implements ILevelMixin {

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
