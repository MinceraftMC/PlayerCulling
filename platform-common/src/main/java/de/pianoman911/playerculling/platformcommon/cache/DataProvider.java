package de.pianoman911.playerculling.platformcommon.cache;


import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public interface DataProvider {

    void world(PlatformWorld world);

    boolean isOpaqueFullCube(int x, int y, int z);
}
