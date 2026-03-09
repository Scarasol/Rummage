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

                                // --- 子分支 3.1: rummage_speed ---
                                .then(Commands.literal("rummage_speed")
                                        .then(Commands.literal("get")
                                                .executes(context -> getPlayerRummageSpeed(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(context -> setPlayerRummageSpeed(
                                                                context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                DoubleArgumentType.getDouble(context, "value")
                                                        ))
                                                )
                                        )
                                )

                                // --- 子分支 3.2: can_chain_rummage ---
                                .then(Commands.literal("can_chain_rummage")
                                        .then(Commands.literal("get")
                                                .executes(context -> getPlayerChainRummage(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setPlayerChainRummage(
                                                                context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                BoolArgumentType.getBool(context, "value")
                                                        ))
                                                )
                                        )
                                )

                                // --- 子分支 3.3: silent_rummage ---
                                .then(Commands.literal("silent_rummage")
                                        .then(Commands.literal("get")
                                                .executes(context -> getPlayerSilentRummage(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setPlayerSilentRummage(
                                                                context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                BoolArgumentType.getBool(context, "value")
                                                        ))
                                                )
                                        )
                                )

                                // --- 子分支 3.4: min_rummage_rarity ---
                                .then(Commands.literal("min_rummage_rarity")
                                        .then(Commands.literal("get")
                                                .executes(context -> getPlayerMinRummageRarity(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(context -> setPlayerMinRummageRarity(
                                                                context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                DoubleArgumentType.getDouble(context, "value")
                                                        ))
                                                )
                                        )
                                )

                                // --- 子分支 3.5: destroy_chance ---
                                .then(Commands.literal("destroy_chance")
                                        .then(Commands.literal("get")
                                                .executes(context -> getPlayerDestroyChance(
                                                        context,
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(context -> setPlayerDestroyChance(
                                                                context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                DoubleArgumentType.getDouble(context, "value")
                                                        ))
                                                )
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

    // ========== Rummage Speed 逻辑 ==========
    private static int getPlayerRummageSpeed(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.RUMMAGE_MODIFIER.get());
            if (attribute != null) {
                double value = attribute.getValue();
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get_speed", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_speed_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerRummageSpeed(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, double value) {
        int successCount = 0;
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.RUMMAGE_MODIFIER.get());
            if (attribute != null) {
                attribute.setBaseValue(value);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set_speed", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_speed_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }

    // ========== Chain Rummage 逻辑 ==========
    private static int getPlayerChainRummage(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.CAN_CHAIN_RUMMAGE.get());
            if (attribute != null) {
                boolean value = attribute.getValue() > 1e-5;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get_chain", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_chain_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerChainRummage(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean value) {
        int successCount = 0;
        double doubleValue = value ? 1.0D : 0.0D;

        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.CAN_CHAIN_RUMMAGE.get());
            if (attribute != null) {
                attribute.setBaseValue(doubleValue);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set_chain", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_chain_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }

    // ========== Silent Rummage 逻辑 ==========
    private static int getPlayerSilentRummage(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.SILENT_RUMMAGE.get());
            if (attribute != null) {
                boolean value = attribute.getValue() > 1e-5;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get_silent", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_silent_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerSilentRummage(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, boolean value) {
        int successCount = 0;
        double doubleValue = value ? 1.0D : 0.0D;

        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.SILENT_RUMMAGE.get());
            if (attribute != null) {
                attribute.setBaseValue(doubleValue);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set_silent", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_silent_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }

    // ========== Min Rummage Rarity 逻辑 ==========
    private static int getPlayerMinRummageRarity(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.MIN_RUMMAGE_RARITY.get());
            if (attribute != null) {
                double value = attribute.getValue();
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get_min_rarity", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_min_rarity_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerMinRummageRarity(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, double value) {
        int successCount = 0;
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.MIN_RUMMAGE_RARITY.get());
            if (attribute != null) {
                attribute.setBaseValue(value);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set_min_rarity", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_min_rarity_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }

    // ========== Destroy Chance 逻辑 ==========
    private static int getPlayerDestroyChance(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.DESTROY_CHANCE.get());
            if (attribute != null) {
                double value = attribute.getValue();
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_get_destroy_chance", player.getDisplayName(), value
                ), false);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_destroy_chance_attribute", player.getDisplayName()
                ));
            }
        }
        return targets.size();
    }

    private static int setPlayerDestroyChance(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, double value) {
        int successCount = 0;
        for (ServerPlayer player : targets) {
            AttributeInstance attribute = player.getAttribute(RummageAttributes.DESTROY_CHANCE.get());
            if (attribute != null) {
                attribute.setBaseValue(value);
                successCount++;
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.rummage.success.player_set_destroy_chance", player.getDisplayName(), value
                ), true);
            } else {
                context.getSource().sendFailure(Component.translatable(
                        "command.rummage.error.no_destroy_chance_attribute", player.getDisplayName()
                ));
            }
        }
        return successCount;
    }
}