package com.scarasol.rummage.util;

import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.init.RummageTags;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.RummageActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端翻找逻辑工具类
 * @author Scarasol
 */
public class ServerRummageUtil {

    /**
     * 处理翻找完成时的逻辑（包含多人状态同步与连锁解锁）
     */
    public static void handleRummageCompletion(ServerPlayer serverPlayer, AbstractContainerMenu menu, Slot targetSlot, RummageTarget target) {
        // 1. 获取需要同步的玩家 UUID
        Set<UUID> syncUUIDs = getPlayersToSync(serverPlayer, target);

        // 2. 预计算连锁条件
        ItemStack targetItem = targetSlot.getItem();
        boolean canChain = canChainRummage(targetItem);

        // 3. 遍历并同步每个玩家的状态
        for (UUID uuid : syncUUIDs) {
            ServerPlayer syncPlayer = serverPlayer.server.getPlayerList().getPlayer(uuid);
            if (syncPlayer != null) {
                // 标记主格子为已翻找
                target.entity().markSlotRummaged(syncPlayer, target.localSlotIndex());

                // 播放音效
                playRummageSound(syncPlayer, targetSlot, target);

                // 获取并同步该玩家的容器菜单
                AbstractContainerMenu menuToSync = (syncPlayer == serverPlayer) ? menu : syncPlayer.containerMenu;
                if (menuToSync != null) {
                    syncPlayerMenuSlots(syncPlayer, menuToSync, targetSlot, target, targetItem, canChain);
                }
            }
        }
    }

    /**
     * 判断哪些玩家需要收到同步数据（兼容 Lootr 的独立战利品逻辑）
     */
    private static Set<UUID> getPlayersToSync(ServerPlayer serverPlayer, RummageTarget target) {
        Set<UUID> syncUUIDs = new HashSet<>();
        String className = target.entity().getClass().getName().toLowerCase(Locale.ROOT);

        if (className.contains("lootr")) {
            syncUUIDs.add(serverPlayer.getUUID()); // Lootr 战利品只同步给自己
        } else {
            syncUUIDs.addAll(target.entity().getRummagingPlayer()); // 否则同步给所有正在翻找该容器的玩家
        }
        return syncUUIDs;
    }

    /**
     * 判断当前物品是否满足连锁翻找的条件
     */
    private static boolean canChainRummage(ItemStack targetItem) {
        if (!CommonConfig.CHAIN_RUMMAGING.get()) return false;
        if (targetItem.is(RummageTags.CHAIN_BLACKLIST)) return false;

        return targetItem.isEmpty() || targetItem.isStackable() || targetItem.is(RummageTags.CHAIN_WHITELIST);
    }

    /**
     * 为指定玩家播放翻找完成音效（带随机音调）
     */
    private static void playRummageSound(ServerPlayer player, Slot targetSlot, RummageTarget target) {
        SoundEvent sound = target.entity().getRummageCompletedSound(targetSlot);
        if (sound != null) {
            float pitch = 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.15F;
            player.playNotifySound(sound, SoundSource.PLAYERS, 0.5F, pitch);
        }
    }

    /**
     * 核心同步逻辑：更新玩家菜单中的格子状态（处理当前格子与连锁格子）
     */
    private static void syncPlayerMenuSlots(ServerPlayer syncPlayer, AbstractContainerMenu menuToSync, Slot targetSlot, RummageTarget target, ItemStack targetItem, boolean canChain) {
        boolean isTargetEmpty = targetItem.isEmpty();

        for (Slot slot : menuToSync.slots) {
            RummageTarget currentSlotTarget = CommonContainerUtil.getTarget(slot, menuToSync);

            // 必须属于同一个实体容器
            if (currentSlotTarget != null && currentSlotTarget.entity().getRummageableUUID().equals(target.entity().getRummageableUUID())) {

                boolean isMainTargetSlot = (slot.index == targetSlot.index);

                if (isMainTargetSlot) {
                    // 同步主目标格子
                    NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> syncPlayer),
                            new RummageActionPacket(slot.index, 2, 0));

                    // 如果不能连锁，处理完主格子就可以直接跳出循环了
                    if (!canChain) {
                        break;
                    }
                } else if (canChain) {
                    // 处理潜在的连锁格子
                    boolean needChain = (isTargetEmpty && slot.getItem().isEmpty())
                            || (!slot.getItem().isEmpty() && slot.getItem().is(targetItem.getItem()));

                    if (needChain && !currentSlotTarget.entity().isSlotRummaged(syncPlayer, currentSlotTarget.localSlotIndex())) {
                        currentSlotTarget.entity().markSlotRummaged(syncPlayer, currentSlotTarget.localSlotIndex());
                        NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> syncPlayer),
                                new RummageActionPacket(slot.index, 2, 0));
                    }
                }
            }
        }
    }
}