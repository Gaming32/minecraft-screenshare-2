package io.github.gaming32.mcscreenshare2;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapId;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ScreenshareCommand {
    public static final DynamicCommandExceptionType NOT_A_DIRECTION = new DynamicCommandExceptionType(dir ->
        Component.literal("Not a direction: " + dir)
    );
    public static final SimpleCommandExceptionType IS_VERTICAL = new SimpleCommandExceptionType(
        Component.literal("Facing direction must not be vertical")
    );
    public static final SimpleCommandExceptionType NOT_FLAT = new SimpleCommandExceptionType(
        Component.literal("Box not flat along direction axis")
    );
    public static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (maximum, specified) -> Component.translatableEscape("commands.fill.toobig", maximum, specified)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("screenshare")
            .then(literal("createbox")
                .then(argument("pos1", BlockPosArgument.blockPos())
                    .then(argument("pos2", BlockPosArgument.blockPos())
                        .then(argument("direction", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Direction.stream().map(Direction::getSerializedName), builder
                            ))
                            .then(argument("backing", BlockStateArgument.block(buildContext))
                                .executes(ScreenshareCommand::createBox)
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
}
