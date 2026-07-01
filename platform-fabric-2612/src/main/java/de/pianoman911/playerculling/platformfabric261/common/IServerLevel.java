package de.pianoman911.playerculling.platformfabric261.common;
// Created by booky10 in PlayerCulling (18:06 14.07.2025)

import de.pianoman911.playerculling.platformfabric261.platform.FabricWorld;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface IServerLevel {

    @Nullable FabricWorld getCullWorld();

    FabricWorld getCullWorldOrCreate();
}
