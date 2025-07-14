package de.pianoman911.playerculling.platformfolianms1216;

import ca.spottedleaf.concurrentutil.util.Priority;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionChunkCache;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformpaper.PlayerCullingPlugin;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlatform;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlayer;
import de.pianoman911.playerculling.platformpaper.platform.PaperWorld;
import de.pianoman911.playerculling.platformpaper.util.PaperNmsAdapter;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.scores.PlayerTeam;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class FoliaNmsAdapterImpl implements PaperNmsAdapter {

    private static final String HANDLER_NAME = "playerculling";

    private final Map<Level, Set<Long>> levels = new ConcurrentHashMap<>();
    private @MonotonicNonNull NmsPacketListener packetListener;

    public FoliaNmsAdapterImpl() {
        if (!PaperNmsAdapter.isFolia() ||
                (SharedConstants.getProtocolVersion() != 771 &&
                        SharedConstants.getProtocolVersion() != 772)
        ) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void init(PlayerCullingPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new EntityTrackerListener(plugin.getCullShip()), plugin);
    }

    @Override
    @SuppressWarnings("resource")
    public void injectNetwork(PlayerCullingPlugin plugin) {
        if (this.packetListener == null) {
            this.packetListener = new NmsPacketListener(plugin.getCullShip());
        }
        NamespacedKey key = new NamespacedKey(plugin, "packets");
        ChannelInitializeListenerHolder.addListener(key, channel -> channel.pipeline()
                .addBefore("packet_handler", HANDLER_NAME, this.packetListener));
        for (Connection connection : MinecraftServer.getServer().getConnection().getConnections()) {
            connection.channel.eventLoop().execute(() ->
                    connection.channel.pipeline().addBefore("packet_handler", HANDLER_NAME, this.packetListener));
        }
    }

    @Override
    @SuppressWarnings("resource")
    public void uninjectNetwork(PlayerCullingPlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "packets");
        ChannelInitializeListenerHolder.removeListener(key);
        for (Connection connection : MinecraftServer.getServer().getConnection().getConnections()) {
            connection.channel.eventLoop().execute(() ->
                    connection.channel.pipeline().remove(HANDLER_NAME));
        }
    }

    @Override
    public void injectWorld(PaperPlatform platform, World world) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        DelegatedChunkPacketBlockController.inject(level, this::onBlockChange);
    }

    @Override
    public void uninjectWorld(World world) {
        // Uninjecting is not needed in Folia
    }

    @Override
    @SuppressWarnings("ConstantConditions") // It can be null
    public @Nullable PlatformChunkAccess provideChunkAccess(PaperPlatform platform, World world, int chunkX, int chunkZ) {
        ChunkAccess chunk = ((CraftWorld) world).getHandle().moonrise$getSpecificChunkIfLoaded(
                chunkX, chunkZ, ChunkStatus.FEATURES);
        return chunk != null ? new FoliaNmsChunkAccess(platform, chunk) : null;
    }

    @Override
    public int getTrackingDistance(World world) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        return level.getServer().getScaledTrackingDistance(level.spigotConfig.playerTrackingRange);
    }

    @Override
    public void tickChangedBlocks(PaperWorld world) {
        Set<Long> blocks = this.getLevelSet(((CraftWorld) world.getWorld()).getHandle());
        OcclusionWorldCache worldCache = world.getOcclusionWorldCache();

        Iterator<Long> it = blocks.iterator();
        while (it.hasNext()) {
            long pos = it.next();
            it.remove();

            int posX = BlockPos.getX(pos);
            int posZ = BlockPos.getZ(pos);
            int chunkX = posX >> 4;
            int chunkZ = posZ >> 4;

            if (!worldCache.hasChunk(chunkX, chunkZ)) {
                continue;
            }

            OcclusionChunkCache chunk = worldCache.chunk(chunkX, chunkZ);
            chunk.recalculateBlock(posX, BlockPos.getY(pos), posZ);
        }
    }

    @Override
    public int getBlockStateCount() {
        return Block.BLOCK_STATE_REGISTRY.size();
    }

    @Override
    public void lazyBuildOcclusionMappings(OcclusionMappings occlusionMappings, PaperWorld world) {
        ServerLevel level = ((CraftWorld) world.getWorld()).getHandle();
        occlusionMappings.lazyBuildCache(index ->
                BlockStateUtil.buildVoxelShape(Block.BLOCK_STATE_REGISTRY.byId(index), level));
    }

    @Override
    @SuppressWarnings({"resource", "UnstableApiUsage", "ConstantConditions"})
    public void addPairing(PlatformPlayer player, PlatformPlayer... targets) {
        ServerPlayer handle = ((CraftPlayer) ((PaperPlayer) player).getDelegate()).getHandle();
        ServerLevel world = handle.level();

        ChunkPos chunkPos = handle.chunkPosition();
        world.moonrise$getChunkTaskScheduler().scheduleChunkTask(chunkPos.x, chunkPos.z, () -> {
            for (PlatformPlayer target : targets) {
                ChunkMap.TrackedEntity tracked = ((CraftPlayer) ((PaperPlayer) target).getDelegate()).getHandle().moonrise$getTrackedEntity();
                if (tracked == null) {
                    continue; // Could be offline or not loaded
                }
                if (tracked.seenBy.add(handle.connection)) {
                    if (PlayerTrackEntityEvent.getHandlerList().getRegisteredListeners().length == 0 || // Refire event
                            (new PlayerTrackEntityEvent(((PaperPlayer) player).getDelegate(), ((PaperPlayer) target).getDelegate()).callEvent())) {
                        tracked.serverEntity.addPairing(handle);
                    }

                    tracked.serverEntity.onPlayerAdd();
                }
            }
        }, Priority.HIGHER);
    }

    @Override
    public @Nullable Vec3d rayTraceBlocks(PaperWorld world, Vec3d start, Vec3d dir, double maxDistance) {
        ServerLevel handle = ((CraftWorld) world.getWorld()).getHandle();

        Vec3 startVec = new Vec3(start.getX(), start.getY(), start.getZ());
        Vec3 endVec = new Vec3(
                start.getX() + dir.getX() * maxDistance,
                start.getY() + dir.getY() * maxDistance,
                start.getZ() + dir.getZ() * maxDistance);

        BlockHitResult result = handle.clip(new ClipContext(startVec, endVec,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (result.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Vec3 pos = result.getLocation();
        return new Vec3d(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean canSeeNametag(Player player, Player target) {
        PlayerTeam targetTeam = MinecraftServer.getServer().getScoreboard().getPlayersTeam(target.getName());
        if (targetTeam == null) {
            return true; // Always visible
        }
        return switch (targetTeam.getNameTagVisibility()) {
            case ALWAYS -> true;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> targetTeam.getPlayers().contains(player.getName());
            case HIDE_FOR_OWN_TEAM -> !targetTeam.getPlayers().contains(player.getName());
        };
    }

    @Override
    public boolean isSpectator(Player player) {
        return ((CraftPlayer) player).getHandle().isSpectator();
    }

    private Set<Long> getLevelSet(Level level) {
        return this.levels.computeIfAbsent(level, __ -> ConcurrentHashMap.newKeySet());
    }

    private void onBlockChange(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        this.getLevelSet(level).add(pos.asLong());
    }
}
