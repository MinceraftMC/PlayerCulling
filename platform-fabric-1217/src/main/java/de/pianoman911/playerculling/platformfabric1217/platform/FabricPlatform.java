package de.pianoman911.playerculling.platformfabric1217.platform;

import de.pianoman911.playerculling.api.PlayerCullingApi;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import de.pianoman911.playerculling.platformfabric1217.PlayerCullingMod;
import de.pianoman911.playerculling.platformfabric1217.common.IServerLevel;
import de.pianoman911.playerculling.platformfabric1217.common.IServerPlayer;
import de.pianoman911.playerculling.platformfabric1217.util.SimpleScheduler;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@NullMarked
public class FabricPlatform implements IPlatform {

    private static @MonotonicNonNull MethodHandle GET_SERVER_PLAYER;
    private final FabricArgumentsProvider argumentsProvider = new FabricArgumentsProvider(this);
    private final PlayerCullingMod mod;
    private final SimpleScheduler scheduler = new SimpleScheduler();
    private final OcclusionMappings occlusionMappings = new OcclusionMappings(Block.BLOCK_STATE_REGISTRY.size() * 8); // 8 voxels per block

    private @MonotonicNonNull MinecraftServer server;
    private @MonotonicNonNull MinecraftServerAudiences audiences;
    private long currentTick;

    public FabricPlatform(PlayerCullingMod mod) {
        this.mod = mod;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlatformWorld getWorld(Key key) {
        ServerLevel world = this.getServer().getLevel((ResourceKey<Level>) key);
        if (world == null) {
            throw new IllegalArgumentException("Can't find world with name " + key);
        }
        return ((IServerLevel) world).getCullWorldOrCreate();
    }

    @Override
    @Unmodifiable
    public Collection<PlatformWorld> getWorlds() {
        List<PlatformWorld> worlds = new ArrayList<>();
        for (ServerLevel level : this.server.getAllLevels()) {
            FabricWorld world = ((IServerLevel) level).getCullWorld();
            if (world != null) {
                worlds.add(world);
            }
        }
        return Collections.unmodifiableList(worlds);
    }

    @Override
    public PlatformPlayer getPlayer(UUID playerId) {
        ServerPlayer player = this.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Can't find player with uuid " + playerId);
        }
        return this.providePlayer(player);
    }

    @Override
    public long getCurrentTick() {
        return this.currentTick;
    }

    @Override
    public void tick() {
        this.currentTick++;
    }

    @Override
    public @Unmodifiable Set<FabricPlayer> getPlayers() {
        Set<FabricPlayer> players = new HashSet<>();
        for (ServerPlayer player : this.getServer().getPlayerList().getPlayers()) {
            players.add(this.providePlayer(player));
        }
        return Collections.unmodifiableSet(players);
    }

    @Override
    public int runTaskLaterAsync(Runnable runnable, long delay) {
        return this.scheduler.scheduleDelayed(runnable, delay);
    }

    @Override
    public int runTaskRepeatingAsync(Runnable runnable, long delay, long period) {
        return this.scheduler.scheduleRepeating(runnable, delay, period);
    }

    @Override
    public void cancelTask(int taskId) {
        this.scheduler.cancel(taskId);
    }

    public void shutdownScheduler() {
        try {
            this.scheduler.shutdown();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void registerApi(PlayerCullingApi api) {
        FabricLoader.getInstance().getObjectShare().put("playerculling:api", api);
    }

    @Override
    public Path getDataFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public YamlConfigHolder<PlayerCullingConfig> loadConfig() {
        return new YamlConfigHolder<>(PlayerCullingConfig.class, this.getDataFolder().resolve("playerculling.yml"));
    }

    @Override
    public FabricArgumentsProvider getArgumentProvider() {
        return this.argumentsProvider;
    }

    public FabricPlayer providePlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return this.providePlayer(serverPlayer);
        } else {
            throw new IllegalArgumentException("Player is not a ServerPlayer");
        }
    }

    public FabricPlayer providePlayer(ServerPlayer player) {
        return ((IServerPlayer) player).getCullPlayerOrCreate();
    }

    @SuppressWarnings("unchecked")
    public <I extends Entity, T extends FabricEntity<I>> T provideEntity(I entity) {
        return (T) switch (entity) {
            case ServerPlayer player -> this.providePlayer(player);
            case LivingEntity livingEntity -> new FabricLivingEntity<>(this, livingEntity);
            default -> new FabricEntity<>(this, entity);
        };
    }

    public <I extends CommandSource> PlatformCommandSender provideCommandSender(I sender) {
        if (sender.getClass().getEnclosingClass() == ServerPlayer.class) { // Anonymous class in ServerPlayer
            if (GET_SERVER_PLAYER == null) {
                GET_SERVER_PLAYER = ReflectionUtil.getGetter(sender.getClass(), ServerPlayer.class, 0);
            }
            try {
                return this.providePlayer((ServerPlayer) GET_SERVER_PLAYER.invoke(sender));
            } catch (Throwable throwable) {
                throw new IllegalStateException(throwable);
            }
        }
        return new FabricCommandSender<>(this, sender);
    }

    public FabricCommandSourceStack provideCommandSourceStack(CommandSourceStack source) {
        return new FabricCommandSourceStack(this, source);
    }

    public void invalidatePlayer(ServerPlayer player) {
        CullShip ship = this.mod.getCullShip();
        ship.removePlayer(player.getUUID());

        // queue invalidation from hidden set
        for (CullPlayer cullPlayer : ship.getPlayers()) {
            cullPlayer.invalidateOther(player.getUUID());
        }
    }

    @SuppressWarnings("deprecation")
    public MinecraftServer getServer() {
        if (this.server == null) {
            this.server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
            this.audiences = MinecraftServerAudiences.of(this.server);
        }
        return this.server;
    }

    public @MonotonicNonNull MinecraftServerAudiences getAudiences() {
        this.getServer(); // try initializing audiences
        return this.audiences;
    }

    public OcclusionMappings getOcclusionMappings() {
        return this.occlusionMappings;
    }

    public PlayerCullingMod getMod() {
        return this.mod;
    }
}
