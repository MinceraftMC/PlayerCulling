package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BlockFace {

    public static final BlockFace UP = new BlockFace(0, 1, 0);
    public static final BlockFace DOWN = new BlockFace(0, -1, 0);
    public static final BlockFace NORTH = new BlockFace(0, 0, -1);
    public static final BlockFace SOUTH = new BlockFace(0, 0, 1);
    public static final BlockFace WEST = new BlockFace(-1, 0, 0);
    public static final BlockFace EAST = new BlockFace(1, 0, 0);

    public final int modX;
    public final int modY;
    public final int modZ;

    private BlockFace(int modX, int modY, int modZ) {
        this.modX = modX;
        this.modY = modY;
        this.modZ = modZ;
    }
}
