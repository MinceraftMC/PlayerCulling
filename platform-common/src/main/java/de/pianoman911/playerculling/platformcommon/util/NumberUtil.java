package de.pianoman911.playerculling.platformcommon.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class NumberUtil {

    private NumberUtil() {
    }

    public static int floor(double num) {
        int floor = (int) num;
        return (double) floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static int ceil(double num) {
        int ceil = (int) num;
        return (double) ceil == num ? ceil : ceil + (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static int roundPositive(double num) {
        return (int) (num + (double) 0.5f);
    }

    public static int round(double num) {
        // inlined version of "return floor(num + (double) 0.5f);"
        int floor = (int) (num + (double) 0.5f);
        return (double) floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }
}
