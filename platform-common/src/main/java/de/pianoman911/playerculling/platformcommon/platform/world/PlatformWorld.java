package de.pianoman911.playerculling.platformcommon.platform.world;

import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.util.TickRefreshSupplier;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;

@NullMarked
public abstract class PlatformWorld<T extends WorldCacheInterface> {

    protected final T cache;
    protected final TickRefreshSupplier<List<PlatformPlayer>> playersInWorld;

    public PlatformWorld(Function<PlatformWorld<T>, T> cache, IPlatform platform) {
        this.cache = cache.apply(this);
        this.playersInWorld = new TickRefreshSupplier<>(platform, this::getPlayers0);
    }

    public T getOcclusionWorldCache() {
        return this.cache;
    }

    @Nullable
    public abstract PlatformChunkAccess getChunkAccess(int x, int z);

    public abstract String getName();

    public abstract Key getKey();

    // Exclusive
    public abstract int getMinY();

    // Exclusive
    public abstract int getMaxY();

    public abstract int getTrackingDistance();

    public abstract int getTrackingDistance(PlatformPlayer player);

    public List<PlatformPlayer> getPlayers() {
        return this.playersInWorld.get();
    }

    public int getPlayerCount() {
        return this.playersInWorld.get().size();
    }

    protected abstract List<PlatformPlayer> getPlayers0();

    @Nullable
    public abstract Vec3d rayTraceBlocks(Vec3d start, Vec3d dir, double maxDistance);

    public abstract void spawnColoredParticle(double x, double y, double z, Color color, float size);

    public void spawnColoredParticle(Vec3d pos, Color color, float size) {
        this.spawnColoredParticle(pos.getX(), pos.getY(), pos.getZ(), color, size);
    }

    public abstract String getBlockStateStringOfBlock(Vec3i blockPos);
}
