package de.pianoman911.playerculling.core.api;

import de.pianoman911.playerculling.api.PlayerCullingApi;
import de.pianoman911.playerculling.core.culling.CullPlayer;
import de.pianoman911.playerculling.core.culling.CullShip;
import de.pianoman911.playerculling.platformcommon.cache.OcclusionWorldCache;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

@NullMarked
public class PlayerCullingApiImpl implements PlayerCullingApi {

    private final CullShip cullShip;

    public PlayerCullingApiImpl(CullShip cullShip) {
        this.cullShip = cullShip;
    }

    @Override
    public boolean isCullingEnabled() {
        return this.cullShip.isCullingEnabled();
    }

    @Override
    public void setCullingEnabled(boolean enabled) {
        this.cullShip.toggleCulling(enabled);
    }

    @Override
    public boolean isCullingEnabled(UUID playerId) {
        CullPlayer player = this.cullShip.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        return player.isCullingEnabled();
    }

    @Override
    public void setCullingEnabled(UUID playerId, boolean enabled) {
        CullPlayer player = this.cullShip.getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.setCullingEnabled(enabled);
    }

    @Override
    public long calcOcclusionCacheBytes() {
        return OcclusionWorldCache.byteSize(this.cullShip.getPlatform().getWorlds());
    }
}
