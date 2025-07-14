package de.pianoman911.playerculling.platformfabric1217.common;
// Created by booky10 in PlayerCulling (18:15 14.07.2025)

import de.pianoman911.playerculling.platformfabric1217.platform.FabricPlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface IServerPlayer {

    @Nullable FabricPlayer getCullPlayer();

    FabricPlayer getCullPlayerOrCreate();
}
