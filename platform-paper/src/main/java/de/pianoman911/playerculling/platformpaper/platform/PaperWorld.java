package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformEntity;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public class PaperWorld extends PlatformWorld {

    private final World world;
    private final PaperPlatform platform;
    private final List<PlatformEntity> loadedEntities = new ArrayList<>();

    public PaperWorld(World world, PaperPlatform platform) {
        super(platform);
        this.world = world;
        this.platform = platform;

        // inject into world to listen to block updates and into entity tracker
        this.platform.getNmsAdapter().injectWorld(this.platform, world);
    }

    @Override
    public @Nullable PlatformChunkAccess getChunkAccess(int x, int z) {
        return this.platform.getNmsAdapter().provideChunkAccess(this.platform, this.world, x, z);
    }

    @Override
    public String getName() {
        return this.world.getName();
    }

    @Override
    public int getMinY() {
        return this.world.getMinHeight();
    }

    @Override
    public int getMaxY() {
        return this.world.getMaxHeight();
    }

    @Override
    public int getTrackingDistance() {
        return this.platform.getNmsAdapter().getTrackingDistance(this.world);
    }

    @Override
    public int getTrackingDistance(PlatformPlayer player) {
        return Math.min(player.getTrackingDistance(), this.getTrackingDistance());
    }

    @Override
    protected List<PlatformPlayer> getPlayers0() {
        List<PlatformPlayer> players = new ArrayList<>(this.world.getPlayerCount());
        for (PaperPlayer platformPlayer : this.platform.getPlayers()) {
            Player paperPlayer = platformPlayer.getDelegate();
            if (paperPlayer.isConnected() && paperPlayer.getWorld() == this.world
                    && !platformPlayer.isSpectator() && !platformPlayer.shouldPreventCulling()) {
                players.add(platformPlayer);
            }
        }
        return players;
    }

    @Override
    protected List<PlatformEntity> getEntities0() {
        synchronized (this.loadedEntities) {
            return List.copyOf(this.loadedEntities);
        }
    }

    public void collectEntities() {
        synchronized (this.loadedEntities) {
            this.loadedEntities.clear();
            for (Entity entity : this.world.getEntities()) {
                PlatformEntity platformEntity = this.platform.provideEntity(entity);
                if (platformEntity instanceof PlatformPlayer player) {
                    if (!player.isOnline() || player.isSpectator() || player.shouldPreventCulling()) {
                        continue;
                    }
                }

                loadedEntities.add(platformEntity);
            }
        }
    }

    @Override
    @Nullable
    public Vec3d rayTraceBlocks(Vec3d start, Vec3d dir, double maxDistance) {
        return this.platform.getNmsAdapter().rayTraceBlocks(this, start, dir, maxDistance);
    }

    @Override
    public void spawnColoredParticle(double x, double y, double z, Color color, float size) {
        int rgb = color.getRGB() & 0x00FFFFFF; // Remove alpha
        // Remove alpha
        Particle.DUST.builder()
                .allPlayers()
                .location(this.world, x, y, z)
                .color(org.bukkit.Color.fromRGB(rgb), size)
                .force(true)
                .spawn();
    }

    @Override
    public String getBlockStateStringOfBlock(Vec3i blockPos) {
        BlockData data = this.world.getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ()).getBlockData();
        return data.getAsString(true);
    }

    public World getWorld() {
        return this.world;
    }
}
