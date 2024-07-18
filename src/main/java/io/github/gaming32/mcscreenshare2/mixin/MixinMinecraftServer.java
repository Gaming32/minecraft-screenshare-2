package io.github.gaming32.mcscreenshare2.mixin;

import io.github.gaming32.mcscreenshare2.MapImage;
import io.github.gaming32.mcscreenshare2.ext.MinecraftServerExt;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements MinecraftServerExt {
    @Unique
    private MapImage mcscreenshare2$mapImage;

    @Override
    public MapImage mcscreenshare2$getMapImage() {
        return mcscreenshare2$mapImage;
    }

    @Override
    public void mcscreenshare2$setMapImage(MapImage image) {
        mcscreenshare2$mapImage = image;
    }
}
