package de.pianoman911.playerculling.platformpaper.platform;

import de.pianoman911.playerculling.api.PlayerCullingApi;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.command.PlatformArgumentProvider;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;
import de.pianoman911.playerculling.platformcommon.util.OcclusionMappings;
import de.pianoman911.playerculling.platformpaper.PlayerCullingPlugin;
import de.pianoman911.playerculling.platformpaper.util.PaperNmsAdapter;
import de.pianoman911.playerculling.platformpaper.util.ServicesUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@NullMarked
public class PaperPlatform implements IPlatform {

    protected final Map<UUID, PaperEntity<?>> entityMap = new ConcurrentHashMap<>(); // Concurrent -> folia support
    protected final Map<UUID, PaperWorld> worldMap = new ConcurrentHashMap<>(); // Concurrent -> folia support
    private final PaperArgumentsProvider argumentsProvider = new PaperArgumentsProvider(this);
    private final Int2ObjectMap<ScheduledTask> taskMap = new Int2ObjectArrayMap<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final OcclusionMappings occlusionMappings;
    private final PlayerCullingPlugin plugin;
    private final PaperNmsAdapter nmsAdapter;
    private long currentTick;

    public PaperPlatform(PlayerCullingPlugin plugin) {
        this.plugin = plugin;
        this.nmsAdapter = ServicesUtil.loadService(PaperNmsAdapter.class);
        this.occlusionMappings = new OcclusionMappings(this.nmsAdapter.getBlockStateCount() * 8); // 8 voxels per block
    }

    @Override
    public void tick() {
        this.currentTick++;
    }

    @Override
    public long getCurrentTick() {
        return this.currentTick;
    }

    @Override
    public PlatformWorld getWorld(Key key) {
        World world = Bukkit.getWorld(key);
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
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Can't find player with uuid " + playerId);
        }
        return (PlatformPlayer) this.provideEntity(player);
    }

    @Override
    public @Unmodifiable Set<PaperPlayer> getPlayers() {
        Set<PaperPlayer> players = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isConnected()) {
                players.add((PaperPlayer) this.provideEntity(player));
            }
        }
        return Collections.unmodifiableSet(players);
    }

    @Override
    public int runTaskLaterAsync(Runnable runnable, long delay) {
        ScheduledTask scheduledTask = this.plugin.getServer().getAsyncScheduler().runDelayed(this.plugin, __ -> runnable.run(), delay, TimeUnit.MILLISECONDS);
        int taskId = this.taskIdCounter.getAndIncrement();
        this.taskMap.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    public int runTaskRepeatingAsync(Runnable runnable, long delay, long period) {
        ScheduledTask scheduledTask = this.plugin.getServer().getAsyncScheduler().runAtFixedRate(this.plugin, __ -> runnable.run(), delay, period, TimeUnit.MILLISECONDS);
        int taskId = this.taskIdCounter.getAndIncrement();
        this.taskMap.put(taskId, scheduledTask);
        return taskId;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void cancelTask(int taskId) {
        ScheduledTask task = this.taskMap.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void registerApi(PlayerCullingApi api) {
        this.plugin.getServer().getServicesManager().register(PlayerCullingApi.class, api, this.plugin, ServicePriority.Normal);
    }

    @Override
    public Path getDataFolder() {
        return this.plugin.getDataPath();
    }

    @Override
    public PlatformArgumentProvider getArgumentProvider() {
        return this.argumentsProvider;
    }

    public PaperWorld provideWorld(World world) {
        return this.worldMap.computeIfAbsent(world.getUID(), id -> {
            PaperWorld paperWorld = new PaperWorld(world, this);
            this.nmsAdapter.lazyBuildOcclusionMappings(this.occlusionMappings, paperWorld);
            return paperWorld;
        });
    }

    public PaperEntity<?> provideEntity(Entity entity) {
        return this.entityMap.computeIfAbsent(entity.getUniqueId(), __ -> {
            if (entity instanceof Player player) {
                return new PaperPlayer(this, player);
            }
            if (entity instanceof LivingEntity livingEntity) {
                return new PaperLivingEntity<>(this, livingEntity);
            }
            return new PaperEntity<>(this, entity);
        });
    }

    @SuppressWarnings("unchecked")
    public <I extends CommandSender, T extends PaperCommandSender<I>> T provideCommandSender(I sender) {
        if (sender instanceof Entity entity) {
            return (T) this.provideEntity(entity);
        }
        return (T) new PaperCommandSender<>(this, sender);
    }

    @SuppressWarnings("UnstableApiUsage")
    public PaperCommandSourceStack provideCommandSourceStack(CommandSourceStack source) {
        return new PaperCommandSourceStack(this, source);
    }

    // TODO: invalidate entities too
    public void invalidatePlayer(Player player) {
        this.entityMap.remove(player.getUniqueId());

        CullShip ship = this.plugin.getCullShip();
        ship.removePlayer(player.getUniqueId());

        // queue invalidation from hidden set
        for (CullPlayer cullPlayer : ship.getPlayers()) {
            cullPlayer.invalidateOther(player.getUniqueId());
        }
    }

    @Unmodifiable
    public Collection<PaperWorld> getPaperWorlds() {
        return Collections.unmodifiableCollection(this.worldMap.values());
    }

    public PlayerCullingPlugin getPlugin() {
        return this.plugin;
    }

    public PaperNmsAdapter getNmsAdapter() {
        return this.nmsAdapter;
    }

    public OcclusionMappings getOcclusionMappings() {
        return this.occlusionMappings;
    }
}
