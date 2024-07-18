package io.github.gaming32.mcscreenshare2.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

public class ScreenshareCodecs {
    public static final Codec<BlockBox> BLOCK_BOX = RecordCodecBuilder.create(instance ->
        instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(BlockBox::min),
            BlockPos.CODEC.fieldOf("max").forGetter(BlockBox::max)
        ).apply(instance, BlockBox::new)
    );
    public static final Codec<Point> POINT = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(p -> (int)p.getX()),
            Codec.INT.fieldOf("y").forGetter(p -> (int)p.getY())
        ).apply(instance, Point::new)
    );
    public static final Codec<Dimension> DIMENSION = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.INT.fieldOf("width").forGetter(d -> (int)d.getWidth()),
            Codec.INT.fieldOf("height").forGetter(d -> (int)d.getHeight())
        ).apply(instance, Dimension::new)
    );
    public static final Codec<Rectangle> RECTANGLE = RecordCodecBuilder.create(
        instance -> instance.group(
            POINT.optionalFieldOf("location", new Point()).forGetter(Rectangle::getLocation),
            DIMENSION.optionalFieldOf("size", new Dimension()).forGetter(Rectangle::getSize)
        ).apply(instance, Rectangle::new)
    );
}
