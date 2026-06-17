package de.pianoman911.playerculling.core.internals;

import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.core.internals.java.cache.ChunkOcclusionDataProvider;
import de.pianoman911.playerculling.core.internals.java.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.core.internals.java.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.platformcommon.internals.OcclusionCullingInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public class InternalsProvider {

    private final CullShip ship;

    public InternalsProvider(CullShip ship) {
        this.ship = ship;
    }

    public OcclusionCullingInterface provideCullingInterface(CullPlayer player) {
        if (this.ship.getNativesAdapter() != null) {
            return this.ship.getNativesAdapter().providerCullingInterface(player.getPlatformPlayer());
        }
        return new OcclusionCullingInstance(new ChunkOcclusionDataProvider(player));
    }

    @SuppressWarnings("unchecked")
    public WorldCacheInterface provideWorldCache(PlatformWorld<?> world) {
        if (this.ship.getNativesAdapter() != null) {
            return this.ship.getNativesAdapter().providerWorldCache(world);
        }
        return new OcclusionWorldCache((PlatformWorld<OcclusionWorldCache>) world);
    }
}
