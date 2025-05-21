package de.pianoman911.playerculling.platformfabric.mixin;

import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.platformfabric.PlayerCullingMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(
            method = "unload(Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At("RETURN")
    )
    public void onUnload(LevelChunk chunk, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        OcclusionWorldCache cache = PlayerCullingMod.getInstance().getPlatform().provideWorld(self).getOcclusionWorldCache();
        cache.removeChunk(chunk.getPos().x, chunk.getPos().z);
    }
}
