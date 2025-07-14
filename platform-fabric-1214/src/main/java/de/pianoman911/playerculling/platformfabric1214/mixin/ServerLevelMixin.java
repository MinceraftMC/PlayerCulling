package de.pianoman911.playerculling.platformfabric1214.mixin;

import de.pianoman911.playerculling.platformfabric1214.PlayerCullingMod;
import de.pianoman911.playerculling.platformfabric1214.platform.FabricPlatform;
import de.pianoman911.playerculling.platformfabric1214.platform.FabricWorld;
import de.pianoman911.playerculling.platformfabric1214.util.BlockStateUtil;
import de.pianoman911.playerculling.platformfabric1214.common.IServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(ServerLevel.class)
@Implements(@Interface(iface = IServerLevel.class, prefix = "playerculling$"))
public abstract class ServerLevelMixin implements IServerLevel {

    @Unique
    private @Nullable FabricWorld world;

    @Inject(
            method = "unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At("RETURN")
    )
    public void onUnload(LevelChunk chunk, CallbackInfo ci) {
        if (this.world != null) {
            ChunkPos chunkPos = chunk.getPos();
            this.world.getOcclusionWorldCache().removeChunk(chunkPos.x, chunkPos.z);
        }
    }

    public @Nullable FabricWorld playerculling$getCullWorld() {
        return this.world;
    }

    public FabricWorld playerculling$getCullWorldOrCreate() {
        if (this.world == null) {
            FabricPlatform platform = PlayerCullingMod.getInstance().getPlatform();
            ServerLevel self = (ServerLevel) (Object) this;
            // construct world
            platform.getOcclusionMappings().lazyBuildCache(index ->
                    BlockStateUtil.buildVoxelShape(Block.BLOCK_STATE_REGISTRY.byId(index), self));
            this.world = new FabricWorld(self, platform);
        }
        return this.world;
    }
}
