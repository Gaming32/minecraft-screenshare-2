package io.github.gaming32.mcscreenshare2.util;

import net.minecraft.core.BlockBox;
import net.minecraft.core.Direction;

public class BlockBoxUtil {
    public static int getCoord(BlockBox box, Direction direction) {
        return (switch (direction.getAxisDirection()) {
            case POSITIVE -> box.max();
            case NEGATIVE -> box.min();
        }).get(direction.getAxis());
    }

    public static int getSize(BlockBox box, Direction.Axis axis) {
        return switch (axis) {
            case X -> box.sizeX();
            case Y -> box.sizeY();
            case Z -> box.sizeZ();
        };
    }

    public static int volume(BlockBox box) {
        return box.sizeX() * box.sizeY() * box.sizeZ();
    }
}
