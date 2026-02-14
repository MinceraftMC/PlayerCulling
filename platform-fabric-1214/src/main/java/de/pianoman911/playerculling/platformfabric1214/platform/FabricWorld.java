package de.pianoman911.playerculling.platformfabric1214.platform;

import de.pianoman911.playerculling.platformcommon.cache.OcclusionChunkCache;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import de.pianoman911.playerculling.platformfabric1214.common.ILevel;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NullMarked
public class FabricWorld extends PlatformWorld {

    private final ServerLevel world;
    private final FabricPlatform platform;

    public FabricWorld(ServerLevel world, FabricPlatform platform) {
        super(platform);
        this.world = world;
        this.platform = platform;
    }

    @Override
    public @Nullable PlatformChunkAccess getChunkAccess(int x, int z) {
        long chunkKey = ChunkPos.asLong(x, z);
        ChunkHolder chunkHolder = this.world.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkKey);
        ChunkAccess chunk = chunkHolder != null ? chunkHolder.getChunkIfPresent(ChunkStatus.FEATURES) : null;
        return chunk != null ? new FabricChunkAccess(this.platform, chunk) : null;
    }

    @Override
    public String getName() {
        return this.world.serverLevelData.getLevelName();
    }

    @Override
    public Key getKey() {
        return this.world.dimension().location().key();
    }

    @Override
    public int getMinY() {
        return this.world.getMinY();
    }

    @Override
    public int getMaxY() {
        return this.world.getMaxY() + 1; // Make inclusive
    }

    @Override
    public int getTrackingDistance() {
        return this.world.getChunkSource().chunkMap.serverViewDistance << 4; // * 16
    }

    @Override
    public int getTrackingDistance(PlatformPlayer player) {
        return this.world.getChunkSource().chunkMap.getPlayerViewDistance(((FabricPlayer) player).getDelegate()) << 4; // * 16
    }

    @Override
    protected List<PlatformPlayer> getPlayers0() {
        List<PlatformPlayer> players = new ArrayList<>();
        for (FabricPlayer platformPlayer : this.platform.getPlayers()) {
            ServerPlayer fabricPlayer = platformPlayer.getDelegate();
            if (fabricPlayer.connection.isAcceptingMessages() && fabricPlayer.level() == this.world
                    && !fabricPlayer.isSpectator() && !platformPlayer.shouldPreventCulling()) {
                players.add(platformPlayer);
            }
        }
        return players;
    }

    @Override
    @Nullable
    public Vec3d rayTraceBlocks(Vec3d start, Vec3d dir, double maxDistance) {
        Vec3 startVec = new Vec3(start.getX(), start.getY(), start.getZ());
        Vec3 endVec = new Vec3(
                start.getX() + dir.getX() * maxDistance,
                start.getY() + dir.getY() * maxDistance,
                start.getZ() + dir.getZ() * maxDistance);

        BlockHitResult result = this.world.clip(new ClipContext(startVec, endVec,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (result.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Vec3 pos = result.getLocation();
        return new Vec3d(pos.x(), pos.y(), pos.z());
    }

    @Override
    public void spawnColoredParticle(double x, double y, double z, Color color, float size) {
        int rgb = color.getRGB() & 0x00FFFFFF; // Remove alpha
        this.world.sendParticles(new DustParticleOptions(rgb, size), true, true, x, y, z, 1, 0, 0, 0, 0);
    }

    @Override
    public String getBlockStateStringOfBlock(Vec3i blockPos) {
        BlockState state = this.world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        StringBuilder bob = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (!state.getValues().isEmpty()) {
            bob.append('[');
            bob.append(state.getValues().entrySet().stream()
                    .map(StateHolder.PROPERTY_ENTRY_TO_STRING_FUNCTION)
                    .collect(Collectors.joining(",")));
            bob.append(']');
        }
        return bob.toString();
    }

    public void tick() {
        ILevel level = (ILevel) this.world;
        for (BlockPos pos : level.getChangedBlocks()) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;

            if (!this.cache.hasChunk(cx, cz)) {
                continue;
            }
            OcclusionChunkCache chunk = this.cache.chunk(cx, cz);
            chunk.recalculateBlock(pos.getX(), pos.getY(), pos.getZ());
        }

        level.getChangedBlocks().clear();
    }

    public ServerLevel getWorld() {
        return this.world;
    }
}
