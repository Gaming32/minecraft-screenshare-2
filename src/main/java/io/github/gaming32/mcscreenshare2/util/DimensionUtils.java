package io.github.gaming32.mcscreenshare2.util;

import java.awt.Rectangle;

public class DimensionUtils {
    public static Rectangle encompass(Rectangle a, Rectangle b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        final int aLeft = a.x;
        final int aTop = a.y;
        final int aRight = aLeft + a.width;
        final int aBottom = aTop + a.height;

        final int bLeft = b.x;
        final int bTop = b.y;
        final int bRight = bLeft + b.width;
        final int bBottom = bTop + b.height;

        final int cLeft = Math.min(aLeft, bLeft);
        final int cTop = Math.min(aTop, bTop);
        final int cRight = Math.max(aRight, bRight);
        final int cBottom = Math.max(aBottom, bBottom);

        return new Rectangle(cLeft, cTop, cRight - cLeft, cBottom - cTop);
    }
}
