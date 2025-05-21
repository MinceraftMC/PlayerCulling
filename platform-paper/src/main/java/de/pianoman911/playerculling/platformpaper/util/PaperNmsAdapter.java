package de.pianoman911.playerculling.platformpaper.util;

import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformChunkAccess;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformpaper.PlayerCullingPlugin;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlatform;
import de.pianoman911.playerculling.platformpaper.platform.PaperWorld;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface PaperNmsAdapter {

    static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    default void init(PlayerCullingPlugin plugin) {
    }

    void injectNetwork(PlayerCullingPlugin plugin);

    void uninjectNetwork(PlayerCullingPlugin plugin);

    void injectWorld(PaperPlatform platform, World world);

    void uninjectWorld(World world);

    @Nullable
    PlatformChunkAccess provideChunkAccess(PaperPlatform platform, World world, int chunkX, int chunkZ);

    int getTrackingDistance(World world);

    void tickChangedBlocks(PaperWorld world);

    int getBlockStateCount();

    void lazyBuildOcclusionMappings(OcclusionMappings occlusionMappings, PaperWorld world);

    void addPairing(PlatformPlayer player, PlatformPlayer... targets);

    @Nullable
    Vec3d rayTraceBlocks(PaperWorld world, Vec3d start, Vec3d dir, double maxDistance);

    boolean canSeeNametag(Player player, Player target);

    boolean isSpectator(Player player);
}
