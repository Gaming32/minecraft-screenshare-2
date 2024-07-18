package io.github.gaming32.mcscreenshare2;

import net.minecraft.core.BlockBox;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class MapWallLocation extends SavedData {
    private static final SavedData.Factory<MapWallLocation> FACTORY = new Factory<>(
        MapWallLocation::new, MapWallLocation::load, null
    );

    private BlockBox range = null;

    public static MapWallLocation get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "screenshare-2-map-wall");
    }

    public BlockBox getRange() {
        return range;
    }

    public void setRange(BlockBox range) {
        this.range = range;
        setDirty();
    }

    @NotNull
    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        if (range != null) {
            compoundTag.put("range", ScreenshareCodecs.BLOCK_BOX.encodeStart(NbtOps.INSTANCE, range).getOrThrow());
        }
        return compoundTag;
    }

    private static MapWallLocation load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        final MapWallLocation result = new MapWallLocation();
        if (compoundTag.contains("range")) {
            ScreenshareCodecs.BLOCK_BOX.parse(NbtOps.INSTANCE, compoundTag.get("range"))
                .ifError(error -> MinecraftScreenshare.LOGGER.error(
                    "Failed to get range field of MapWallLocation: {}", error.message())
                )
                .resultOrPartial()
                .ifPresent(range -> result.range = range);
        }
        return result;
    }
}
