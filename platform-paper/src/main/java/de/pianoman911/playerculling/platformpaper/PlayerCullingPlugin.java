package de.pianoman911.playerculling.platformpaper;

import de.pianoman911.playerculling.core.commands.PlayerCullingCommand;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformpaper.platform.PaperCommandSourceStack;
import de.pianoman911.playerculling.platformpaper.platform.PaperPlatform;
import de.pianoman911.playerculling.platformpaper.platform.PaperWorld;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerCullingPlugin extends JavaPlugin {

    private final PaperPlatform platform;
    private @MonotonicNonNull CullShip cullShip;

    public PlayerCullingPlugin() {
        // build platform implementation in separate method to allow
        // for other platforms extending this platform
        this.platform = this.buildPlatform();
    }

    @Override
    public void onLoad() {
        this.cullShip = new CullShip(this.getPlatform());
        new Metrics(this, 25595);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        this.cullShip.enable();
        this.getPlatform().getNmsAdapter().init(this);

        Bukkit.getPluginManager().registerEvents(new PlayerCullingListener(this), this);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            this.cullShip.addPlayer(new CullPlayer(this.getPlatform().providePlayer(onlinePlayer)));
        }

        // inject into netty pipeline
        this.getPlatform().getNmsAdapter().injectNetwork(this);

        // register commands using paper command api
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(PlayerCullingCommand.createConverted(
                        this.cullShip,
                        platform -> ((PaperCommandSourceStack) platform).getPaperSourceStack(),
                        this.getPlatform()::provideCommandSourceStack
                ))
        );
    }

    @Override
    public void onDisable() {
        // uninject packet listener
        this.getPlatform().getNmsAdapter().uninjectNetwork(this);
        // uninject entity tracker
        for (PaperWorld world : this.getPlatform().getPaperWorlds()) {
            this.getPlatform().getNmsAdapter().uninjectWorld(world.getWorld());
        }
    }

    protected PaperPlatform buildPlatform() {
        return new PaperPlatform(this);
    }

    public PaperPlatform getPlatform() {
        return this.platform;
    }

    public CullShip getCullShip() {
        return this.cullShip;
    }
}
