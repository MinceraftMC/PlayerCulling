package de.pianoman911.playerculling.platformcommon.cache;


import de.pianoman911.playerculling.platformcommon.platform.world.PlatformWorld;

public interface DataProvider {

    void updatePos(PlatformWorld world, double x, double y, double z);

    boolean isOpaqueFullCube(int x, int y, int z);

    int getPlayerViewDistance();
}
