package io.github.gaming32.mcscreenshare2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;

public class ScreenshareCodecs {
    public static final Codec<BlockBox> BLOCK_BOX = RecordCodecBuilder.create(instance ->
        instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(BlockBox::min),
            BlockPos.CODEC.fieldOf("max").forGetter(BlockBox::max)
        ).apply(instance, BlockBox::new)
    );
}
