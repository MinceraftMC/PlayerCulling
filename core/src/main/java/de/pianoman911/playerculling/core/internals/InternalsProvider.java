package de.pianoman911.playerculling.core.internals;

import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.core.internals.java.cache.ChunkOcclusionDataProvider;
import de.pianoman911.playerculling.core.internals.java.cache.OcclusionWorldCache;
import de.pianoman911.playerculling.core.internals.java.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.platformcommon.internals.DataProviderInterface;
import de.pianoman911.playerculling.platformcommon.internals.OcclusionCullingInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public class InternalsProvider {

    private final CullShip ship;

    public InternalsProvider(CullShip ship) {
        this.ship = ship;
    }

    public OcclusionCullingInterface provideCullingInterface(DataProviderInterface dataProvider) {
        if (this.ship.getNativesAdapter() != null) {
            return this.ship.getNativesAdapter().providerCullingInterface();
        }
        return new OcclusionCullingInstance(dataProvider);
    }

    @SuppressWarnings("unchecked")
    public WorldCacheInterface provideWorldCache(PlatformWorld<?> world) {
        if (this.ship.getNativesAdapter() != null) {
            return this.ship.getNativesAdapter().providerWorldCache(world);
        }
        return new OcclusionWorldCache((PlatformWorld<OcclusionWorldCache>) world);
    }

    public DataProviderInterface provideDataProvider(CullPlayer player) {
        if (this.ship.getNativesAdapter() != null) {
            return this.ship.getNativesAdapter().providerDataProvider(player.getPlatformPlayer());
        }
        return new ChunkOcclusionDataProvider(player);
    }
}
