package de.pianoman911.playerculling.platformfabric1219.mixin;

import com.mojang.authlib.GameProfile;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformfabric1219.PlayerCullingMod;
import de.pianoman911.playerculling.platformfabric1219.common.IServerPlayer;
import de.pianoman911.playerculling.platformfabric1219.platform.FabricPlatform;
import de.pianoman911.playerculling.platformfabric1219.platform.FabricPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Implements(@Interface(iface = IServerPlayer.class, prefix = "playerculling$"))
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements IServerPlayer {

    @Unique
    private @Nullable FabricPlayer player;

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile); // dummy ctor
    }

    @Shadow
    public abstract Entity getCamera();

    @Inject(
            method = "setCamera(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN")
    )
    public void setCamera(Entity entity, CallbackInfo info) {
        CullShip ship = PlayerCullingMod.getInstance().getCullShip();
        CullPlayer player = ship.getPlayer(this.getUUID());
        if (player != null) {
            player.setSpectating(this.getCamera() != this);
        }
    }

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL")
    )
    private void onDeathRestore(ServerPlayer from, boolean keepInventory, CallbackInfo ci) {
        FabricPlayer fromPlayer = ((IServerPlayer) from).getCullPlayer();
        if (fromPlayer != null) {
            fromPlayer.replacePlayer((ServerPlayer) (Object) this);
            this.player = fromPlayer;
        }
    }

    public @Nullable FabricPlayer playerculling$getCullPlayer() {
        return this.player;
    }

    public FabricPlayer playerculling$getCullPlayerOrCreate() {
        if (this.player == null) {
            FabricPlatform platform = PlayerCullingMod.getInstance().getPlatform();
            this.player = new FabricPlayer(platform, (ServerPlayer) (Object) this);
        }
        return this.player;
    }
}
