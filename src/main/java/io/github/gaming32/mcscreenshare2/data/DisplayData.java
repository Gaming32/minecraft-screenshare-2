package io.github.gaming32.mcscreenshare2.data;

import io.github.gaming32.mcscreenshare2.MinecraftScreenshare;
import io.github.gaming32.mcscreenshare2.util.ScreenshareCodecs;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

public class DisplayData extends SavedData {
    private static final Factory<DisplayData> FACTORY = new Factory<>(
        DisplayData::createDefault, DisplayData::load, null
    );

    private Rectangle area;

    public static DisplayData createDefault() {
        final DisplayData result = new DisplayData();
        result.setArea(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds()
        );
        return result;
    }

    public static DisplayData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "screenshare-2-display");
    }

    public Rectangle getArea() {
        return area;
    }

    public void setArea(Rectangle area) {
        this.area = area;
        setDirty();
    }

    @NotNull
    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.put("area", ScreenshareCodecs.RECTANGLE.encodeStart(NbtOps.INSTANCE, area).getOrThrow());
        return compoundTag;
    }

    private static DisplayData load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        final DisplayData result = new DisplayData();
        if (compoundTag.contains("area")) {
            ScreenshareCodecs.RECTANGLE.parse(NbtOps.INSTANCE, compoundTag.get("area"))
                .ifError(error -> MinecraftScreenshare.LOGGER.error(
                    "Failed to get area field of DisplayData: {}", error.message())
                )
                .resultOrPartial()
                .ifPresent(area -> result.area = area);
        }
        return result;
    }
}
