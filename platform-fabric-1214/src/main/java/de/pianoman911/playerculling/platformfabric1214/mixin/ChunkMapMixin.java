package de.pianoman911.playerculling.platformfabric1214.mixin;

import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.platformfabric1214.PlayerCullingMod;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@NullMarked
@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Unique
    private static boolean isVisible(ServerPlayer player, Entity target) {
        CullPlayer cullPlayer = PlayerCullingMod.getInstance().getCullShip().getPlayer(player.getUUID());
        return cullPlayer == null || !cullPlayer.isHidden(target.getUUID());
    }

    @Redirect(
            method = "addEntity(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "NEW",
                    target = "net/minecraft/server/level/ChunkMap$TrackedEntity")
    )
    private ChunkMap.TrackedEntity injectCustomEntity(ChunkMap this$0, Entity entity, int range, int updateInterval, boolean trackDelta) {
        if (!(entity instanceof ServerPlayer)) {
            return this$0.new TrackedEntity(entity, range, updateInterval, trackDelta);
        }
        return this$0.new TrackedEntity(entity, range, updateInterval, trackDelta) {
            @Override
            public void updatePlayer(ServerPlayer player) {
                if (player == this.entity) {
                    return;
                }

                if (isVisible(player, this.entity)) {
                    super.updatePlayer(player);
                } else if (this.seenBy.remove(player.connection)) {
                    this.serverEntity.removePairing(player);
                }
            }
        };
    }
}
