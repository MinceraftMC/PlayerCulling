package de.pianoman911.playerculling.platformpapernms1216;

import com.destroystokyo.paper.util.SneakyThrow;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import de.pianoman911.playerculling.platformcommon.util.WaypointMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Set;

public class DelegatedWaypointManager extends ServerWaypointManager {

    private static final MethodHandle SET_WAYPOINT_MANAGER = ReflectionUtil.getSetter(ServerLevel.class, ServerWaypointManager.class, 0);

    private static final MethodHandle GET_WAYPOINTS = ReflectionUtil.getGetter(ServerWaypointManager.class, Set.class, 0);
    private static final MethodHandle GET_PLAYERS = ReflectionUtil.getGetter(ServerWaypointManager.class, Set.class, 1);
    private static final MethodHandle GET_CONNECTIONS = ReflectionUtil.getGetter(ServerWaypointManager.class, Table.class, 0);

    private static final MethodHandle SET_WAYPOINTS = ReflectionUtil.getSetter(ServerWaypointManager.class, Set.class, 0);
    private static final MethodHandle SET_PLAYERS = ReflectionUtil.getSetter(ServerWaypointManager.class, Set.class, 1);
    private static final MethodHandle SET_CONNECTIONS = ReflectionUtil.getSetter(ServerWaypointManager.class, Table.class, 0);

    private static final MethodHandle CREATE_CONNECTION = ReflectionUtil.getVoidMethod(ServerWaypointManager.class,
            "createConnection", ServerPlayer.class, WaypointTransmitter.class);
    private static final MethodHandle UPDATE_CONNECTION = ReflectionUtil.getVoidMethod(ServerWaypointManager.class,
            "updateConnection", ServerPlayer.class, WaypointTransmitter.class, WaypointTransmitter.Connection.class);

    private final ServerWaypointManager original;
    private final CullShip ship;

    public DelegatedWaypointManager(ServerWaypointManager original, CullShip ship) {
        this.original = original;
        this.ship = ship;

        // Copy the original state
        copyState(original, this);

        ship.getConfig().addReloadHookAndRun(__ -> {
            this.breakAllConnections();
            for (WaypointTransmitter waypoint : this.this$waypoints()) {
                this.remakeConnections(waypoint);
            }
        });
    }

    public static void inject(ServerLevel level, CullShip ship) {
        ServerWaypointManager original = level.getWaypointManager();
        DelegatedWaypointManager delegated = new DelegatedWaypointManager(original, ship);

        try {
            SET_WAYPOINT_MANAGER.invoke(level, delegated);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    public static void uninject(ServerLevel level) {
        ServerWaypointManager delegated = level.getWaypointManager();
        if (!(delegated instanceof DelegatedWaypointManager manager)) {
            return;
        }
        try {
            SET_WAYPOINT_MANAGER.invoke(level, manager.getOriginalModified());
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    private static void copyState(ServerWaypointManager original, ServerWaypointManager delegated) {
        try {
            SET_WAYPOINTS.invoke(delegated, GET_WAYPOINTS.invoke(original));
            SET_PLAYERS.invoke(delegated, GET_PLAYERS.invoke(original));
            SET_CONNECTIONS.invoke(delegated, GET_CONNECTIONS.invoke(original));
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    private static boolean isLocatorBarEnabledFor(ServerPlayer player) {
        return player.level().getServer().getGameRules().getBoolean(GameRules.RULE_LOCATOR_BAR);
    }

    public ServerWaypointManager getOriginalModified() {
        // Return the original manager with the modified state
        copyState(this, this.original);
        return this.original;
    }

    public void disconnectWaypoint(ServerPlayer player, WaypointTransmitter waypoint) {
        WaypointTransmitter.Connection connection = this.this$connections().remove(player, waypoint);
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void trackWaypoint(WaypointTransmitter waypoint) {
        if (!(waypoint instanceof ServerPlayer)) {
            super.trackWaypoint(waypoint);
            return; // Only override player waypoints
        }
        this.this$waypoints().add(waypoint);
        for (ServerPlayer receiver : this.this$players()) {
            this.override$createConnection(receiver, waypoint);
        }
    }

    @Override
    public void updateWaypoint(WaypointTransmitter waypoint) {
        if (!this.this$waypoints().contains(waypoint)) {
            return;
        }
        if (!(waypoint instanceof ServerPlayer target)) {
            super.updateWaypoint(waypoint);
            return; // Only override player waypoints
        }
        Map<ServerPlayer, WaypointTransmitter.Connection> receiverConnections = Tables.transpose(this.this$connections()).row(waypoint);
        Sets.SetView<ServerPlayer> newReceivers = Sets.difference(this.this$players(), receiverConnections.keySet());

        for (Map.Entry<ServerPlayer, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(receiverConnections.entrySet())) {
            this.override$updateConnection(entry.getKey(), target, entry.getValue());
        }

        for (ServerPlayer receiver : newReceivers) {
            this.override$createConnection(receiver, waypoint);
        }
    }

    @Override
    public void addPlayer(ServerPlayer player) {
        this.this$players().add(player);

        for (WaypointTransmitter waypoint : this.this$waypoints()) {
            if ((waypoint instanceof ServerPlayer)) {
                this.override$createConnection(player, waypoint);
            } else {
                this.super$createConnection(player, waypoint);
            }
        }
        if (player.isTransmittingWaypoint()) {
            this.trackWaypoint(player);
        }
    }

    @Override
    public void updatePlayer(ServerPlayer player) {
        Map<WaypointTransmitter, WaypointTransmitter.Connection> receiverConnections = this.this$connections().row(player);
        Sets.SetView<WaypointTransmitter> newWaypoints = Sets.difference(this.this$waypoints(), receiverConnections.keySet());

        for (Map.Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(receiverConnections.entrySet())) {
            if (entry.getKey() instanceof ServerPlayer target) {
                this.override$updateConnection(player, target, entry.getValue());
            } else {
                this.super$updateConnection(player, entry.getKey(), entry.getValue());
            }
        }

        for (WaypointTransmitter waypoint : newWaypoints) {
            if (waypoint instanceof ServerPlayer) {
                this.override$createConnection(player, waypoint);
            } else {
                this.super$createConnection(player, waypoint);
            }
        }
    }

    @Override
    public void remakeConnections(WaypointTransmitter waypoint) {
        if (!(waypoint instanceof ServerPlayer)) {
            super.remakeConnections(waypoint);
            return; // Only override player waypoints
        }
        for (ServerPlayer receiver : this.this$players()) {
            this.override$createConnection(receiver, waypoint);
        }
    }

    protected Set<WaypointTransmitter> this$waypoints() {
        try {
            return (Set<WaypointTransmitter>) GET_WAYPOINTS.invoke(this);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            return null; // Unreachable, but required by the compiler
        }
    }

    protected Set<ServerPlayer> this$players() {
        try {
            return (Set<ServerPlayer>) GET_PLAYERS.invoke(this);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            return null; // Unreachable, but required by the compiler
        }
    }

    protected Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> this$connections() {
        try {
            return (Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection>) GET_CONNECTIONS.invoke(this);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            return null; // Unreachable, but required by the compiler
        }
    }

    protected void super$createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        try {
            CREATE_CONNECTION.invoke(this.original, player, waypoint);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    protected void super$updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
        try {
            UPDATE_CONNECTION.invoke(this.original, player, waypoint, connection);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    protected void override$createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        if (player == waypoint) {
            return;
        }
        if (!isLocatorBarEnabledFor(player)) {
            return;
        }
        this.handleUpdateWaypointConnection(player, waypoint);
    }

    protected void override$updateConnection(ServerPlayer player, ServerPlayer waypoint, WaypointTransmitter.Connection connection) {
        if (player == waypoint) {
            return;
        }
        if (!isLocatorBarEnabledFor(player)) {
            return;
        }
        if (this.ship.getConfig().getDelegate().waypointMode == WaypointMode.CULLED_AZIMUTH) {
            CullPlayer cullPlayer = this.ship.getPlayer(player.getUUID());
            if (cullPlayer == null) {
                return; // cull player is null, ignore
            }
            if (cullPlayer.isHidden(waypoint.getUUID())) {
                this.disconnectWaypoint(player, waypoint);
                return;
            }
        }
        if (WaypointTransmitter.doesSourceIgnoreReceiver(waypoint, player)) { // Reduced "broken" check
            this.handleUpdateWaypointConnection(player, waypoint); // Recreate the connection if it's "broken" - Is "broken" a goofy name mojang, isn't it? Why not just call it "invalid"?
        } else {
            connection.update();
        }
    }

    protected void handleUpdateWaypointConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        WaypointTransmitter.Connection connection = switch (this.ship.getConfig().getDelegate().waypointMode) {
            case WaypointMode.AZIMUTH -> new WaypointTransmitter.EntityAzimuthConnection(
                    (ServerPlayer) waypoint, new Waypoint.Icon().cloneAndAssignStyle((LivingEntity) waypoint), player);
            case WaypointMode.CULLED_AZIMUTH -> {
                if (!(waypoint instanceof ServerPlayer target)) {
                    yield null;
                }
                CullPlayer cullPlayer = this.ship.getPlayer(player.getUUID());
                if (cullPlayer == null) {
                    yield null; // cull player is null, ignore
                }
                yield cullPlayer.isHidden(target.getUUID()) ?
                        null : new WaypointTransmitter.EntityAzimuthConnection(
                        target, new Waypoint.Icon().cloneAndAssignStyle(target), player);
            }
            case WaypointMode.VANILLA -> waypoint.makeWaypointConnectionWith(player).orElse(null);
            default -> null;
        };

        if (connection == null) {
            WaypointTransmitter.Connection removed = this.this$connections().remove(player, waypoint);
            if (removed != null) {
                removed.disconnect();
            }
        } else {
            this.this$connections().put(player, waypoint, connection);
            connection.connect();
        }
    }
}
