package de.pianoman911.playerculling.api;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

@NullMarked
public interface PlayerCullingApi {

    /**
     * Returns whether culling is globally enabled.
     *
     * @return true if culling is enabled, false otherwise
     */
    boolean isCullingEnabled();

    /**
     * Sets whether culling is globally enabled.
     *
     * @param enabled true to enable culling, false to disable it
     */
    void setCullingEnabled(boolean enabled);

    /**
     * Returns whether culling is enabled for the given player.
     *
     * @param playerId the unique ID of the player
     * @return true if culling is enabled for the player, false otherwise
     */
    boolean isCullingEnabled(UUID playerId);

    /**
     * Sets whether culling is enabled for the given player.
     *
     * @param playerId the unique ID of the player
     * @param enabled  true to enable culling, false to disable it
     */
    void setCullingEnabled(UUID playerId, boolean enabled);

    /**
     * Returns a set of player IDs that are hidden from the given player.
     *
     * @param playerId the unique ID of the player
     * @return an immutable set of player IDs that are hidden from the player
     */
    @Unmodifiable
    Set<UUID> getHiddenPlayers(UUID playerId);

    /**
     * Calculates the number of bytes used by the occlusion cache.
     *
     * @return bytes
     */
    long calcOcclusionCacheBytes();
}
