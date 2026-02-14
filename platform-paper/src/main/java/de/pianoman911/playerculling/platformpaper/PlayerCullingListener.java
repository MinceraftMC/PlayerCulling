package de.pianoman911.playerculling.platformpaper;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformpaper.platform.PaperWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class PlayerCullingListener implements Listener {

    private final PlayerCullingPlugin plugin;

    public PlayerCullingListener(PlayerCullingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlatformPlayer platformPlayer = this.plugin.getPlatform().providePlayer(player);

        CullShip cullShip = this.plugin.getCullShip();
        cullShip.addPlayer(new CullPlayer(this.plugin.getCullShip(), platformPlayer));
        cullShip.getUpdater().onJoin(platformPlayer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getPlatform().invalidatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        this.plugin.getPlatform().tick();
        for (PaperWorld world : this.plugin.getPlatform().getPaperWorlds()) {
            this.plugin.getPlatform().getNmsAdapter().tickChangedBlocks(world);
        }
    }

    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        CullPlayer player = this.plugin.getCullShip().getPlayer(event.getPlayer().getUniqueId());
        if (player == null) {
            return; // cull player is null, ignore
        }
        player.setSpectating(false);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        PaperWorld world = this.plugin.getPlatform().provideWorld(event.getWorld());
        world.getOcclusionWorldCache().removeChunk(event.getChunk().getX(), event.getChunk().getZ());
    }
}
