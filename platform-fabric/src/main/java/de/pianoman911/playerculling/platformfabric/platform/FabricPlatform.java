package de.pianoman911.playerculling.platformfabric.platform;

import de.pianoman911.playerculling.api.PlayerCullingApi;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.util.ReflectionUtil;
import de.pianoman911.playerculling.platformcommon.config.PlayerCullingConfig;
import de.pianoman911.playerculling.platformcommon.config.YamlConfigHolder;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformCommandSender;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import de.pianoman911.playerculling.platformfabric.PlayerCullingMod;
import de.pianoman911.playerculling.platformfabric.util.BlockStateUtil;
import de.pianoman911.playerculling.platformfabric.util.SimpleScheduler;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.key.Key;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@NullMarked
public class FabricPlatform implements IPlatform {

    private static @MonotonicNonNull MethodHandle GET_SERVER_PLAYER;
    private final Map<ServerLevel, FabricWorld> worldMap = new WeakHashMap<>();
    private final Map<ServerPlayer, FabricPlayer> playerMap = new WeakHashMap<>();
    private final FabricArgumentsProvider argumentsProvider = new FabricArgumentsProvider(this);
    private final PlayerCullingMod mod;
    private final SimpleScheduler scheduler = new SimpleScheduler();
    private final OcclusionMappings occlusionMappings = new OcclusionMappings(Block.BLOCK_STATE_REGISTRY.size() * 8); // 8 voxels per block
    private @MonotonicNonNull MinecraftServer server;
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
        return this.provideWorld(world);
    }

    @Override
    @Unmodifiable
    public Collection<PlatformWorld> getWorlds() {
        return Collections.unmodifiableCollection(this.worldMap.values());
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

    public FabricWorld provideWorld(Level level) {
        if (level instanceof ServerLevel world) {
            return this.provideWorld(world);
        } else {
            throw new IllegalArgumentException("Level is not a ServerLevel");
        }
    }

    public FabricWorld provideWorld(ServerLevel world) {
        return this.worldMap.computeIfAbsent(world, id -> {
            this.occlusionMappings.lazyBuildCache(index ->
                    BlockStateUtil.buildVoxelShape(Block.BLOCK_STATE_REGISTRY.byId(index), id));
            return new FabricWorld(id, this);
        });
    }

    public FabricPlayer providePlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return this.providePlayer(serverPlayer);
        } else {
            throw new IllegalArgumentException("Player is not a ServerPlayer");
        }
    }

    public FabricPlayer providePlayer(ServerPlayer player) {
        return this.playerMap.computeIfAbsent(player, id -> new FabricPlayer(this, id));
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
        this.playerMap.remove(player);

        CullShip ship = this.mod.getCullShip();
        ship.removePlayer(player.getUUID());

        // queue invalidation from hidden set
        for (CullPlayer cullPlayer : ship.getPlayers()) {
            cullPlayer.invalidateOther(player.getUUID());
        }
    }

    public void replacePlayer(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        FabricPlayer player = this.playerMap.remove(oldPlayer);
        if (player != null) {
            player.replacePlayer(newPlayer);
            this.playerMap.put(newPlayer, player);
        }
    }

    @SuppressWarnings("deprecation")
    public MinecraftServer getServer() {
        if (this.server == null) {
            this.server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        }
        return this.server;
    }

    @Unmodifiable
    public Collection<FabricWorld> getFabricWorlds() {
        return Collections.unmodifiableCollection(this.worldMap.values());
    }

    public OcclusionMappings getOcclusionMappings() {
        return this.occlusionMappings;
    }

    public PlayerCullingMod getMod() {
        return this.mod;
    }
}
