package de.pianoman911.playerculling.platformcommon.platform;

import de.pianoman911.playerculling.api.PlayerCullingApi;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformArgumentProvider;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@NullMarked
public interface IPlatform {

    void tick();

    long getCurrentTick();

    PlatformWorld getWorld(Key key);

    @Unmodifiable
    Collection<PlatformWorld> getWorlds();

    PlatformPlayer getPlayer(UUID playerId);

    @Unmodifiable
    Set<? extends PlatformPlayer> getPlayers();

    int runTaskLaterAsync(Runnable runnable, long delay);

    int runTaskRepeatingAsync(Runnable runnable, long delay, long period);

    void cancelTask(int taskId);

    void registerApi(PlayerCullingApi api);

    Path getDataFolder();

    PlatformArgumentProvider getArgumentProvider();

    default YamlConfigHolder<PlayerCullingConfig> loadConfig() {
        return new YamlConfigHolder<>(PlayerCullingConfig.class, this.getDataFolder().resolve("config.yml"));
    }
}
