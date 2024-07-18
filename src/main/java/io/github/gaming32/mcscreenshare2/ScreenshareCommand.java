package io.github.gaming32.mcscreenshare2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.gaming32.mcscreenshare2.data.DisplayData;
import io.github.gaming32.mcscreenshare2.data.MapWallLocation;
import io.github.gaming32.mcscreenshare2.util.BlockBoxUtil;
import io.github.gaming32.mcscreenshare2.util.DimensionUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ScreenshareCommand {
    public static final DynamicCommandExceptionType NOT_A_DIRECTION = new DynamicCommandExceptionType(dir ->
        Component.literal("Not a direction: " + dir)
    );
    public static final SimpleCommandExceptionType IS_VERTICAL = new SimpleCommandExceptionType(
        Component.literal("Facing direction must not be vertical")
    );
    public static final SimpleCommandExceptionType DISPLAY_NOT_FOUND = new SimpleCommandExceptionType(
        Component.literal("Specified display not found")
    );
    public static final SimpleCommandExceptionType NOT_FLAT = new SimpleCommandExceptionType(
        Component.literal("Box not flat along direction axis")
    );
    public static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (maximum, specified) -> Component.translatableEscape("commands.fill.toobig", maximum, specified)
    );

    private static final int DISPLAY_DEFAULT = 0;
    private static final int DISPLAY_ALL = -1;

    private static final Comparator<Rectangle> BOUNDS_COMPARATOR = Comparator.comparing(
        Rectangle::getLocation, Comparator.comparingDouble(Point::getX).thenComparingDouble(Point::getY)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("screenshare")
            .then(literal("createbox")
                .then(argument("pos1", BlockPosArgument.blockPos())
                    .then(argument("pos2", BlockPosArgument.blockPos())
                        .then(argument("direction", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Direction.stream()
                                    .filter(dir -> !dir.getAxis().isVertical())
                                    .map(Direction::getSerializedName),
                                builder
                            ))
                            .then(argument("backing", BlockStateArgument.block(buildContext))
                                .executes(ScreenshareCommand::createBox)
                            )
                        )
                    )
                )
            )
            .then(literal("display")
                .then(argument("display", IntegerArgumentType.integer(1))
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        IntStream.rangeClosed(1, GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length)
                            .mapToObj(Integer::toString),
                        builder
                    ))
                    .executes(ctx -> setDisplay(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "display")))
                )
                .then(literal("default")
                    .executes(ctx -> setDisplay(ctx.getSource(), DISPLAY_DEFAULT))
                )
                .then(literal("all")
                    .executes(ctx -> setDisplay(ctx.getSource(), DISPLAY_ALL))
                )
                .then(literal("bounds")
                    .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                            .then(argument("width", IntegerArgumentType.integer(128))
                                .then(argument("height", IntegerArgumentType.integer(128))
                                    .executes(ScreenshareCommand::setDisplayBounds)
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private static int createBox(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
        final BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");
        final String directionName = StringArgumentType.getString(context, "direction");
        final Direction direction = Direction.CODEC.byName(directionName);
        if (direction == null) {
            throw NOT_A_DIRECTION.create(directionName);
        }
        final BlockInput backing = BlockStateArgument.getBlock(context, "backing");

        if (direction.getAxis().isVertical()) {
            throw IS_VERTICAL.create();
        }

        final BlockBox box = BlockBox.of(pos1, pos2);
        if (BlockBoxUtil.getSize(box, direction.getAxis()) != 1) {
            throw NOT_FLAT.create();
        }

        final ServerLevel level = context.getSource().getLevel();

        final int volume = BlockBoxUtil.volume(box);
        final int maxVolume = level.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
        if (volume > maxVolume) {
            throw ERROR_AREA_TOO_LARGE.create(maxVolume, volume);
        }

        for (final BlockPos pos : box) {
            Clearable.tryClear(level.getBlockEntity(pos));
            backing.place(level, pos, Block.UPDATE_CLIENTS);

            final ItemFrame frame = new ItemFrame(level, pos.relative(direction), direction);
            level.addFreshEntity(frame);

            final ItemStack itemStack = Items.FILLED_MAP.getDefaultInstance();
            itemStack.set(DataComponents.MAP_ID, level.getFreeMapId());
            frame.setItem(itemStack);
        }

        MapWallLocation.get(level).setRange(box.move(direction, 1));

        context.getSource().sendSuccess(() -> Component.literal("Set up screen in level"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDisplay(CommandSourceStack source, int display) throws CommandSyntaxException {
        final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final Rectangle bounds = switch (display) {
            case DISPLAY_DEFAULT -> graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            case DISPLAY_ALL -> {
                Rectangle rectangle = null;
                for (final GraphicsDevice device : graphicsEnvironment.getScreenDevices()) {
                    rectangle = DimensionUtils.encompass(rectangle, device.getDefaultConfiguration().getBounds());
                }
                yield rectangle;
            }
            default -> {
                final GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();
                if (display > devices.length) {
                    throw DISPLAY_NOT_FOUND.create();
                }
                final Rectangle[] allBounds = new Rectangle[devices.length];
                for (int i = 0; i < devices.length; i++) {
                    allBounds[i] = devices[i].getDefaultConfiguration().getBounds();
                }
                Arrays.sort(allBounds, BOUNDS_COMPARATOR);
                yield allBounds[display - 1];
            }
        };
        return setBounds(source, bounds);
    }

    private static int setDisplayBounds(CommandContext<CommandSourceStack> context) {
        final int x = IntegerArgumentType.getInteger(context, "x");
        final int y = IntegerArgumentType.getInteger(context, "y");
        final int width = IntegerArgumentType.getInteger(context, "width");
        final int height = IntegerArgumentType.getInteger(context, "height");
        return setBounds(context.getSource(), new Rectangle(x, y, width, height));
    }

    private static int setBounds(CommandSourceStack source, Rectangle bounds) {
        DisplayData.get(source.getServer()).setArea(bounds);
        source.sendSuccess(() -> Component.literal("Set display bounds to " + bounds), true);
        return Command.SINGLE_SUCCESS;
    }
}
