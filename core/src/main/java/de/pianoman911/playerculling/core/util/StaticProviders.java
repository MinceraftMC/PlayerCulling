package de.pianoman911.playerculling.core.util;

import de.pianoman911.playerculling.core.occlusion.OcclusionCullingInstance;
import de.pianoman911.playerculling.platformcommon.cache.DataProvider;
import de.pianoman911.playerculling.platformcommon.occlusion.OcclusionCullingInterface;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.NullMarked;

import java.util.function.Function;

@NullMarked
public final class StaticProviders {

    private static Function<DataProvider, OcclusionCullingInterface> OCCLUSION_CULLING_INTERFACE = OcclusionCullingInstance::new;

    public static OcclusionCullingInterface provideOcclusionInterface(DataProvider provider) {
        return OCCLUSION_CULLING_INTERFACE.apply(provider);
    }

    public static void replaceOcclusionInterfaceProvider(@UnknownNullability Function<DataProvider, OcclusionCullingInterface> provider) {
        if (provider != null) {
            OCCLUSION_CULLING_INTERFACE = provider;
        }
    }
}
