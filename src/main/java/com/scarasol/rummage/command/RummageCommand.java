package com.scarasol.rummage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.scarasol.rummage.api.mixin.IRummageable;
import com.scarasol.rummage.init.RummageAttributes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

/**
 * @author Scarasol
 */
public class RummageCommand {

    private static final SimpleCommandExceptionType ERROR_NOT_RUMMAGEABLE_BLOCK = new SimpleCommandExceptionType(Component.translatable("command.rummage.error.not_rummageable_block"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rummage")
                .requires(source -> source.hasPermission(2))

                // ==================== 分支 1: /rummage block ====================
                .then(Commands.literal("block")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(context -> setBlockRummageState(
                                                context,
                                                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                BoolArgumentType.getBool(context, "state")
                                        ))
                                )
                        )
                )

                // ==================== 分支 2: /rummage entity ====================
                .then(Commands.literal("entity")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(context -> setEntityRummageState(
                                                context,
                                                EntityArgument.getEntities(context, "targets"),
                                                BoolArgumentType.getBool(context, "state")
                                        ))
                                )
                        )
                )

                // ==================== 分支 3: /rummage player ====================
                .then(Commands.literal("player")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("get")
                                        .executes(context -> getPlayerRummageModifier(
                                                context,
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                .executes(context -> setPlayerRummageModifier(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        DoubleArgumentType.getDouble(context, "value")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int setBlockRummageState(CommandContext<CommandSourceStack> context, BlockPos pos, boolean state) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof IRummageable rummageable) {
            rummageable.setNeedRummage(state);
            blockEntity.setChanged();
            rummageable.getFullyRummagedPlayer().clear();
            rummageable.getRummageProgress().clear();
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.rummage.success.block", pos.getX(), pos.getY(), pos.getZ(), state
            ), true);
            return 1;
        } else {
            throw ERROR_NOT_RUMMAGEABLE_BLOCK.create();
        }
    }

    private static int setEntityRummageState(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets, boolean state) {
        int successCount = 0;

        for (Entity entity : targets) {
            if (entity instanceof IRummageable rummageable) {
                rummageable.setNeedRummage(state);
                rummageable.getFullyRummagedPlayer().clear();
                rummageable.getRummageProgress().clear();
                successCount++;
            }
        }

        if (successCount == 0) {
            context.getSource().sendFailure(Component.translatable("command.rummage.error.no_rummageable_entity"));
        } else {
            final int finalCount = successCount;
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.rummage.success.entity", finalCount, state
            ), true);
        }

        return successCount;
    }

    private static int getPlayerRummageModifier(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.RUMMAGE_MODIFIER.get());
            if (attribute != null) {
                double value = attribute.getValue();
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerRummageModifier(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, double value) {
        int successCount = 0;
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.RUMMAGE_MODIFIER.get());
            if (attribute != null) {
                attribute.setBaseValue(value);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }
}