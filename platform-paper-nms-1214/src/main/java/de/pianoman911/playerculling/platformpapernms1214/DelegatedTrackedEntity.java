package de.pianoman911.playerculling.platformpapernms1214;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import com.destroystokyo.paper.util.SneakyThrow;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.util.ForwardedInt2ObjectMap;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spigotmc.AsyncCatcher;

import java.lang.invoke.MethodHandle;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class DelegatedTrackedEntity {

    // ChunkMap setter
    private static final MethodHandle SET_ENTITY_MAP = ReflectionUtil.getSetter(ChunkMap.class, Int2ObjectMap.class, 0);

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

    public static void injectChunkMap(ChunkMap chunkMap, CullShip ship) {
        try {
            SET_ENTITY_MAP.invoke(chunkMap, new CustomEntityMap(chunkMap, ship));
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
    }

    public static void uninjectChunkMap(ChunkMap map) {
        if (!(map.entityMap instanceof CustomEntityMap customMap)) {
            return; // not injected
        }
        try {
            customMap.uninject();
            SET_ENTITY_MAP.invoke(map, customMap.getDelegate());
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
        }
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

    // Custom entity map for injecting the custom tracked entity only for players
    private static final class CustomEntityMap extends ForwardedInt2ObjectMap<ChunkMap.TrackedEntity> {

        private final Int2ObjectMap<ChunkMap.TrackedEntity> uninjectedMap = new Int2ObjectOpenHashMap<>();
        private final ChunkMap chunkMap;
        private final CullShip ship;
        private ChunkMap.@Nullable TrackedEntity nextInsert;

        public CustomEntityMap(ChunkMap chunkMap, CullShip ship) {
            super(chunkMap.entityMap);
            this.chunkMap = chunkMap;
            this.ship = ship;
        }

        public void uninject() {
            this.getDelegate().putAll(this.uninjectedMap);
            this.uninjectedMap.clear();
        }

        @Override
        public ChunkMap.TrackedEntity put(int entityId, ChunkMap.TrackedEntity entity) {
            try {
                Entity mcEntity = (Entity) GET_ENTITY.invoke(entity);
                if (!(mcEntity instanceof ServerPlayer)) {
                    return super.put(entityId, entity);
                }
                if (this.nextInsert != null) {
                    throw new IllegalStateException("PlayerCulling failed to inject into entity tracker, "
                            + "please report this error!");
                }
                // queue insert, wait until moonrise has fully initialized the tracked entity
                this.nextInsert = entity;
                return null;
            } catch (Throwable throwable) {
                SneakyThrow.sneaky(throwable);
                throw new AssertionError();
            }
        }

        private void fixInsert() {
            if (this.nextInsert == null) {
                return; // no insert queued
            }

            try {
                Entity mcEntity = (Entity) GET_ENTITY.invoke(this.nextInsert);
                // construct delegate which implements player culling
                ChunkMap.TrackedEntity delegate = constructDelegate(this.chunkMap, this.nextInsert, this.ship);
                if (delegate != this.nextInsert) {
                    this.uninjectedMap.put(mcEntity.getId(), this.nextInsert);
                    mcEntity.moonrise$setTrackedEntity(delegate);
                }
                super.put(mcEntity.getId(), delegate);
            } catch (Throwable throwable) {
                SneakyThrow.sneaky(throwable);
            } finally {
                this.nextInsert = null;
            }
        }

        @Override
        public ChunkMap.TrackedEntity remove(int key) {
            this.uninjectedMap.remove(key);
            return super.remove(key);
        }

        @Override
        @NotNull
        public ObjectCollection<ChunkMap.TrackedEntity> values() {
            this.fixInsert();
            return super.values();
        }
    }
}
