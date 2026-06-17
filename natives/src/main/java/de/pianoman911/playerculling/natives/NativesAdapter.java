package de.pianoman911.playerculling.natives;

import de.pianoman911.playerculling.platformcommon.internals.OcclusionCullingInterface;
import de.pianoman911.playerculling.platformcommon.internals.WorldCacheInterface;
import de.pianoman911.playerculling.platformcommon.platform.entity.PlatformPlayer;
import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public interface NativesAdapter {

    OcclusionCullingInterface providerCullingInterface(PlatformPlayer player);

    WorldCacheInterface providerWorldCache(PlatformWorld<?> worldy);
}
