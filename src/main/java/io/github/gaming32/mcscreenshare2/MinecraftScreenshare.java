package io.github.gaming32.mcscreenshare2;

import com.mojang.logging.LogUtils;
import io.github.gaming32.mcscreenshare2.data.DisplayData;
import io.github.gaming32.mcscreenshare2.data.MapWallLocation;
import io.github.gaming32.mcscreenshare2.ext.MinecraftServerExt;
import io.github.gaming32.mcscreenshare2.ext.ServerCommonPacketListenerImplExt;
import io.github.gaming32.mcscreenshare2.util.BlockBoxUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class MinecraftScreenshare implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        if (GraphicsEnvironment.isHeadless()) {
            LOGGER.error("Headless environment detected. Screenshares are impossible.");
            return;
        }
        final Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            LOGGER.error("Robot not available. Cannot do screenshares.");
            return;
        }
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            updateScreenData(robot, server);
            updatePing(server);
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if ((level.getServer().getTickCount() & 2) != 0 || level.players().isEmpty()) return;
            final MapImage image = ((MinecraftServerExt)level.getServer()).mcscreenshare2$getMapImage();
            if (image == null) return;
            final MapWallLocation location = MapWallLocation.get(level);
            final BlockBox mapBox = location.getRange();
            if (mapBox == null) return;
            final var frames = level.getEntities(
                EntityType.ITEM_FRAME, mapBox.aabb(),
                frame -> frame.getDirection().getAxis() != Direction.Axis.Y && frame.getItem().is(Items.FILLED_MAP)
            );
            for (final ServerPlayer player : level.players()) {
                ((ServerCommonPacketListenerImplExt)player.connection).mcscreenshare2$updateShouldFrameBeSent();
            }
            for (final ItemFrame frame : frames) {
                final BlockPos framePos = frame.getPos();
                final Direction rightDir = frame.getDirection().getClockWise();
                final int frameX = Math.abs(framePos.get(rightDir.getAxis()) - BlockBoxUtil.getCoord(mapBox, rightDir));
                if (frameX * 128 >= image.getWidth()) continue;
                final int frameY = mapBox.max().getY() - framePos.getY();
                if (frameY * 128 >= image.getHeight()) continue;

                final var packet = new ClientboundMapItemDataPacket(
                    frame.getItem().get(DataComponents.MAP_ID),
                    (byte)0,
                    true,
                    Optional.empty(),
                    Optional.of(image.getMapPatch(frameX * 128, frameY * 128))
                );
                for (final ServerPlayer player : level.players()) {
                    if (((ServerCommonPacketListenerImplExt)player.connection).mcscreenshare2$shouldFrameBeSent()) {
                        player.connection.send(packet);
                    }
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            ScreenshareCommand.register(dispatcher, registryAccess)
        );
    }

    private static void updateScreenData(Robot robot, MinecraftServer server) {
        if ((server.getTickCount() & 2) != 0 && server.getPlayerCount() == 0) return;
        final Rectangle area = DisplayData.get(server).getArea();
        CompletableFuture.runAsync(() -> {
            final int width = area.width;
            final int height = area.height;
            final BufferedImage image = robot.createScreenCapture(area);
            final MapImage result = new MapImage(width, height);
            final int[] rgb = image.getRGB(0, 0, width, height, null, 0, width);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result.setPixelColor(x, y, rgb[x + y * width]);
                }
            }
            ((MinecraftServerExt)server).mcscreenshare2$setMapImage(result);
        });
    }

    private static void updatePing(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) return;
        final int pingId = ThreadLocalRandom.current().nextInt();
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            final long time = System.currentTimeMillis();
            ((ServerCommonPacketListenerImplExt)player.connection).mcscreenshare2$getLastPings().put(pingId, time);
            player.connection.send(new ClientboundPingPacket(pingId));
        }
    }
}
