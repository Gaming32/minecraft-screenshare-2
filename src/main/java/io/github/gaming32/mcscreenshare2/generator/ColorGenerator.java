package io.github.gaming32.mcscreenshare2.generator;

import net.minecraft.world.level.material.MapColor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;

public class ColorGenerator {
    private static final int MAP_COLOR_COUNT = 62 * 4;
    private static final int RGB_COLOR_COUNT = 256 * 256 * 256;
    private static final String FILE_PATH = "src/main/resources/screenshare-color-cache.dat";
    private static final int BUFFER_SIZE = 16384;

    public static void main(String[] args) throws IOException {
        System.out.println("Generating color cache");
        final long start, end;
        try (OutputStream os = new FileOutputStream(FILE_PATH)) {
            start = System.nanoTime();
            generate(os);
            end = System.nanoTime();
        }
        System.out.println("Generated color cache in " + Duration.ofNanos(end - start));
    }

    private static void generate(OutputStream os) throws IOException {
        final int[] mapColors = new int[MAP_COLOR_COUNT];
        for (int i = 0; i < MAP_COLOR_COUNT; i++) {
            mapColors[i] = bgr(MapColor.getColorFromPackedId(i));
        }
        final byte[] buffer = new byte[BUFFER_SIZE];
        int bufferPos = 0;
        for (int i = 0; i < RGB_COLOR_COUNT; i++) {
            buffer[bufferPos++] = findMapColor(0xff000000 | i, mapColors);
            if (bufferPos == BUFFER_SIZE) {
                os.write(buffer);
                bufferPos = 0;
            }
        }
    }

    private static byte findMapColor(int argb, int[] mapColors) {
        int index = 0;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 4; i < MAP_COLOR_COUNT; i++) {
            final double distance = colorDistance(argb, mapColors[i]);
            if (distance < best) {
                best = distance;
                index = i;
            }
        }
        return (byte)index;
    }

    private static double colorDistance(int c1, int c2) {
        final double rmean = (r(c1) + r(c2)) / 2.0;
        final double r = r(c1) - r(c2);
        final double g = g(c1) - g(c2);
        final int b = b(c1) - b(c2);
        final double weightR = 2 + rmean / 256.0;
        final double weightG = 4.0;
        final double weightB = 2 + (255 - rmean) / 256.0;
        return weightR * r * r + weightG * g * g + weightB * b * b;
    }

    private static int bgr(int rgb) {
        return (rgb & 0xff00ff00) |
               ((rgb & 0xff0000) >>> 16) |
               ((rgb & 0xff) << 16);
    }

    private static int a(int argb) {
        return argb >>> 24;
    }

    private static int r(int argb) {
        return (argb >>> 16) & 0xff;
    }

    private static int g(int argb) {
        return (argb >>> 8) & 0xff;
    }

    private static int b(int argb) {
        return argb & 0xff;
    }
}
