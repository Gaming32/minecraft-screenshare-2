package io.github.gaming32.mcscreenshare2;

import net.minecraft.Util;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class MapImage {
    private static final byte[] COLOR_DATA;

    static {
        try (InputStream is = MapImage.class.getResourceAsStream("/screenshare-color-cache.dat")) {
            if (is == null) {
                throw new IllegalStateException("Missing screenshare-color-cache.dat");
            }
            COLOR_DATA = is.readAllBytes();
            if (COLOR_DATA.length != 256 * 256 * 256) {
                throw new IllegalStateException("COLOR_DATA does not have colors for every RGB color");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final byte[] data;
    private final int width;
    private final int height;

    public MapImage(int width, int height) {
        data = new byte[width * height];
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte getPixel(int x, int y) {
        return data[index(x, y)];
    }

    public void setPixel(int x, int y, byte pixel) {
        data[index(x, y)] = pixel;
    }

    public int getPixelColor(int x, int y) {
        return FastColor.ABGR32.fromArgb32(MapColor.getColorFromPackedId(getPixel(x, y)));
    }

    public void setPixelColor(int x, int y, int argb) {
        setPixel(x, y, getMapColor(argb));
    }

    public MapItemSavedData.MapPatch getMapPatch(int startX, int startY) {
        final int endX = Math.min(width, startX + 128);
        final int endY = Math.min(height, startY + 128);
        final int partWidth = endX - startX;
        final int partHeight = endY - startY;

        final byte[] dst = new byte[partWidth * partHeight];
        int srcI = index(startX, startY);
        int dstI = 0;
        for (int i = 0; i < partHeight; i++) {
            try {
                System.arraycopy(data, srcI, dst, dstI, partWidth);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw e;
            }
            srcI += width;
            dstI += partWidth;
        }

        return new MapItemSavedData.MapPatch(0, 0, partWidth, partHeight, dst);
    }

    private int index(int x, int y) {
        return x + y * width;
    }

    private static byte getMapColor(int argb) {
        if (FastColor.ARGB32.alpha(argb) < 128) {
            return 0;
        }
        return COLOR_DATA[argb & 0xffffff];
    }
}
