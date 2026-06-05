package de.pianoman911.playerculling.natives;

import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.occlusion.OcclusionCullingInterface;

import java.util.function.Function;

public interface NativesAdapter {

    Function<DataProvider, OcclusionCullingInterface> providerCullingInterface();
}
