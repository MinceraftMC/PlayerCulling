package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StringUtil {

    private StringUtil() {
    }

    public static String toNumInUnits(long bytes) {
        int u = 0;
        for (; bytes > 1024 * 1024; bytes >>= 10) {
            u++;
        }
        if (bytes > 1024) {
            u++;
        }
        return String.format("%.2f %ciB", bytes / 1024f, "kkMGTPE".charAt(u));
    }
}
