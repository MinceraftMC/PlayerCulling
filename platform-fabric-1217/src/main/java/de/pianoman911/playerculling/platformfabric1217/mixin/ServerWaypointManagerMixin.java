package de.pianoman911.playerculling.platformfabric1217.mixin;
// Created by booky10 in PlayerCulling (21:57 14.07.2025)

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformfabric1217.PlayerCullingMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

@NullMarked
@Mixin(ServerWaypointManager.class)
public abstract class ServerWaypointManagerMixin {

    @Unique
    private final CullShip ship = PlayerCullingMod.getInstance().getCullShip();

    @Shadow @Final
    private Set<WaypointTransmitter> waypoints;

    @Shadow
    public abstract void breakAllConnections();

    @Shadow
    public abstract void remakeConnections(WaypointTransmitter waypoint);

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void postInit(CallbackInfo ci) {
        this.ship.getConfig().addReloadHookAndRun(__ -> {
            this.breakAllConnections();
            for (WaypointTransmitter waypoint : this.waypoints) {
                this.remakeConnections(waypoint);
            }
        });
    }

    @WrapOperation(
            method = {"createConnection", "updateConnection"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/waypoints/WaypointTransmitter;makeWaypointConnectionWith(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"
            ),
            expect = 2
    )
    private Optional<WaypointTransmitter.Connection> wrapWaypoint(WaypointTransmitter waypoint, ServerPlayer player, Operation<Optional<WaypointTransmitter.Connection>> original) {
        if (!(waypoint instanceof ServerPlayer target)) {
            return original.call(waypoint, player);
        }
        return Optional.ofNullable(this.createConnection(player, target));
    }

    @WrapOperation(
            method = "updateConnection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/waypoints/WaypointTransmitter$Connection;isBroken()Z"
            )
    )
    private boolean wrapBrokenCheck(
            WaypointTransmitter.Connection instance,
            Operation<Boolean> original,
            @Local(argsOnly = true) ServerPlayer player,
            @Local(argsOnly = true) WaypointTransmitter waypoint
    ) {
        if (!(waypoint instanceof ServerPlayer target)) {
            return original.call(instance);
        }
        return switch (this.ship.getConfig().getDelegate().waypointMode) {
            case CULLED_AZIMUTH -> {
                CullPlayer cullPlayer = this.ship.getPlayer(player.getUUID());
                // broken if cull player is null or target is hidden
                yield cullPlayer == null || cullPlayer.isHidden(target.getUUID());
            }
            // don't break if transmitter is near the receiver, only break if transmitter ignores receiver
            case AZIMUTH -> WaypointTransmitter.doesSourceIgnoreReceiver(target, player);
            case VANILLA -> original.call(instance);
            case HIDDEN -> true; // always broken
        };
    }

    @Unique
    private WaypointTransmitter.@Nullable Connection createConnection(ServerPlayer player, ServerPlayer waypoint) {
        switch (this.ship.getConfig().getDelegate().waypointMode) {
            case CULLED_AZIMUTH: {
                CullPlayer cullPlayer = this.ship.getPlayer(player.getUUID());
                if (cullPlayer == null || cullPlayer.isHidden(waypoint.getUUID())) {
                    // cull player is null or target is hidden, no connection
                    return null;
                }
                // fall through
            }
            case AZIMUTH:
                return new WaypointTransmitter.EntityAzimuthConnection(
                        waypoint, new Waypoint.Icon().cloneAndAssignStyle(waypoint), player);
            case VANILLA:
                return waypoint.makeWaypointConnectionWith(player).orElse(null);
            case HIDDEN:
                return null;
            default:
                throw new AssertionError();
        }
    }
}
