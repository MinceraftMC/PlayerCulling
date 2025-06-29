package de.pianoman911.playerculling.platformfolianms1216;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import com.destroystokyo.paper.util.SneakyThrow;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;
import org.spigotmc.AsyncCatcher;

import java.lang.invoke.MethodHandle;
import java.util.Set;

@NullMarked
public final class DelegatedTrackedEntity {

    // TrackedEntity getter
    private static final MethodHandle GET_ENTITY = ReflectionUtil.getGetter(ChunkMap.TrackedEntity.class, Entity.class, 0);
    private static final MethodHandle GET_RANGE = ReflectionUtil.getGetter(ChunkMap.TrackedEntity.class, int.class, 0);
    private static final MethodHandle GET_LAST_SECTION_POS = ReflectionUtil.getGetter(ChunkMap.TrackedEntity.class, SectionPos.class, 0);
    private static final MethodHandle GET_LAST_CHUNK_UPDATE = ReflectionUtil.getGetter(ChunkMap.TrackedEntity.class, long.class, 0);
    private static final MethodHandle GET_LAST_TRACKED_CHUNK = ReflectionUtil.getGetter(ChunkMap.TrackedEntity.class, NearbyPlayers.TrackedChunk.class, 0);

    // TrackedEntity setter
    private static final MethodHandle SET_SERVER_ENTITY = ReflectionUtil.getSetter(ChunkMap.TrackedEntity.class, ServerEntity.class, 0);
    private static final MethodHandle SET_LAST_SECTION_POS = ReflectionUtil.getSetter(ChunkMap.TrackedEntity.class, SectionPos.class, 0);
    private static final MethodHandle SET_SEEN_BY = ReflectionUtil.getSetter(ChunkMap.TrackedEntity.class, Set.class, 0);
    private static final MethodHandle SET_LAST_CHUNK_UPDATE = ReflectionUtil.getSetter(ChunkMap.TrackedEntity.class, long.class, 0);
    private static final MethodHandle SET_LAST_TRACKED_CHUNK = ReflectionUtil.getSetter(ChunkMap.TrackedEntity.class, NearbyPlayers.TrackedChunk.class, 0);

    // ServerEntity getter
    private static final MethodHandle GET_UPDATE_INTERVAL = ReflectionUtil.getGetter(ServerEntity.class, int.class, 3);
    private static final MethodHandle GET_TRACK_DELTA = ReflectionUtil.getGetter(ServerEntity.class, boolean.class, 0);

    private DelegatedTrackedEntity() {
    }

    public static ChunkMap.TrackedEntity constructDelegate(ChunkMap map, ChunkMap.TrackedEntity entity, CullShip ship) throws Throwable {
        Entity mcEntity = (Entity) GET_ENTITY.invoke(entity);
        if (!(mcEntity instanceof ServerPlayer)) {
            return entity; // skip useless delegation
        }
        int range = (int) GET_RANGE.invoke(entity);
        int updateInterval = (int) GET_UPDATE_INTERVAL.invoke(entity.serverEntity);
        boolean trackDelta = (boolean) GET_TRACK_DELTA.invoke(entity.serverEntity);

        // Anonymous class due non-static inner class
        ChunkMap.TrackedEntity delegated = map.new TrackedEntity(mcEntity, range, updateInterval, trackDelta) {

            private final CullPlayer cullPlayer = ship.getPlayer(mcEntity.getUUID());
            private final MethodHandle getCullPlayer = ReflectionUtil.getGetter(this.getClass(), CullPlayer.class, 0);

            @Override
            public void updatePlayer(@NotNull ServerPlayer player) {
                AsyncCatcher.catchOp("player tracker update");
                if (player == mcEntity) {
                    return;
                }
                ChunkMap.TrackedEntity trackedPlayer = player.moonrise$getTrackedEntity();
                if (!this.getClass().isInstance(trackedPlayer)) {
                    // not a delegated entity, skip
                    super.updatePlayer(player);
                    return;
                }
                try {
                    CullPlayer cullPlayer = (CullPlayer) getCullPlayer.invoke(trackedPlayer);

                    // check if player culling allows seeing this player
                    if (!cullPlayer.isHidden(mcEntity.getUUID())) {
                        // not culled, delegate
                        super.updatePlayer(player);
                    } else if (this.seenBy.remove(player.connection)) {
                        this.serverEntity.removePairing(player);
                    }
                } catch (Throwable throwable) {
                    SneakyThrow.sneaky(throwable);
                }
            }
        };
        // copy over fields from delegate
        SET_SERVER_ENTITY.invoke(delegated, entity.serverEntity);
        SET_LAST_SECTION_POS.invoke(delegated, GET_LAST_SECTION_POS.invoke(entity));
        SET_SEEN_BY.invoke(delegated, entity.seenBy);
        SET_LAST_CHUNK_UPDATE.invoke(delegated, GET_LAST_CHUNK_UPDATE.invoke(entity));
        SET_LAST_TRACKED_CHUNK.invoke(delegated, GET_LAST_TRACKED_CHUNK.invoke(entity));
        return delegated;
    }

    private static boolean isVisible(ServerPlayer player, Entity target, CullShip ship) {
        CullPlayer cullPlayer = ship.getPlayer(player.getUUID());
        return cullPlayer == null || !cullPlayer.isHidden(target.getUUID());
    }

    public static void injectPlayer(@NotNull Player player, CullShip ship) throws Throwable {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        ChunkMap.TrackedEntity custom = constructDelegate(handle.level().chunkSource.chunkMap, handle.moonrise$getTrackedEntity(), ship);
        handle.moonrise$setTrackedEntity(custom);
    }
}
