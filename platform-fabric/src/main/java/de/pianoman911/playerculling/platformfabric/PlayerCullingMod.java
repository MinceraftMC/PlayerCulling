package de.pianoman911.playerculling.platformfabric;

import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformfabric.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;

public class PlayerCullingMod implements ModInitializer {

    private static PlayerCullingMod INSTANCE; // Fabric forces me to make static abuse :(

    private final FabricPlatform platform = new FabricPlatform(this);
    private CullShip cullTask;

    public PlayerCullingMod() {
        INSTANCE = this;
    }

    public static PlayerCullingMod getInstance() {
        return INSTANCE;
    }

    @Override
    public void onInitialize() {
        this.cullTask = new CullShip(this.platform);
        new PlayerCullingListener(this).register();
    }

    public FabricPlatform getPlatform() {
        return this.platform;
    }

    public CullShip getCullShip() {
        return this.cullTask;
    }
}
