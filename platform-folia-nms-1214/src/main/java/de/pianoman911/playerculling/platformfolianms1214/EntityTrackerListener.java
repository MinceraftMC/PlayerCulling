package de.pianoman911.playerculling.platformfolianms1214;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import de.pianoman911.playerculling.core.culling.CullShip;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class EntityTrackerListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerCulling");

    private final CullShip ship;
    private final Set<Player> players = Collections.newSetFromMap(new WeakHashMap<>());

    public EntityTrackerListener(CullShip ship) {
        this.ship = ship;
    }

    @EventHandler
    public void onAdd(PlayerTrackEntityEvent event) {
        if (this.players.add(event.getPlayer())) {
            try {
                DelegatedTrackedEntity.injectPlayer(event.getPlayer(), this.ship);
            } catch (Throwable exception) {
                LOGGER.error("Failed to inject player into entity tracker", exception);
                event.getPlayer().kick(Component.text("[PlayerCulling] Failed to inject player into entity tracker"));
            }
        }
    }

    @EventHandler
    public void onSwitchWorld(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Player player) {
            this.players.remove(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.players.remove(player);
    }
}
