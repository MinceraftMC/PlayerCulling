package de.pianoman911.playerculling.platformcommon.platform.world;

import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.platformcommon.platform.IPlatform;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.util.TickRefreshSupplier;
import de.pianoman911.playerculling.platformcommon.vector.Vec3d;
import de.pianoman911.playerculling.platformcommon.vector.Vec3i;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.List;

@NullMarked
public abstract class PlatformWorld {

    protected final OcclusionWorldCache cache = new OcclusionWorldCache(this);
    protected final TickRefreshSupplier<List<PlatformPlayer>> playersInWorld;

    public PlatformWorld(IPlatform platform) {
        this.playersInWorld = new TickRefreshSupplier<>(platform, this::getPlayers0);
    }

    public OcclusionWorldCache getOcclusionWorldCache() {
        return this.cache;
    }

    @Nullable
    public abstract PlatformChunkAccess getChunkAccess(int x, int z);

    public abstract String getName();

    public abstract int getMinY();

    public abstract int getMaxY();

    public abstract int getHeight();

    public abstract int getTrackingDistance();

    public abstract int getTrackingDistance(PlatformPlayer player);

    public List<PlatformPlayer> getPlayers() {
        return this.playersInWorld.get();
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
