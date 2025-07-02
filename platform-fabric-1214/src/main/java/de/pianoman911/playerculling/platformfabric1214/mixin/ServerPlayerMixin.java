package de.pianoman911.playerculling.platformfabric1214.mixin;

import de.pianoman911.playerculling.platformfabric1214.event.CameraEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(
            method = "setCamera(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN")
    )
    public void setCamera(Entity entity, CallbackInfo info) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        CameraEvent.START_STOP_SPECTATING.invoker().onStartStopSpectating(self, self.getCamera() != self);
    }
}
