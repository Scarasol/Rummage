package com.scarasol.rummage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scarasol.rummage.api.mixin.IRummageMenu;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.RummageActionPacket;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

/**
 * @author Scarasol
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements IRummageMenu {

    @Shadow @Final public NonNullList<Slot> slots;
    @Unique private Player rummage$activePlayer;

    // 状态机变量
    @Unique private int rummage$clientHoveredSlot = -1;
    @Unique private int rummage$currentTargetSlot = -1;
    @Unique private int rummage$progress = 0;
    @Unique private int rummage$totalTime = 0;

    @Override
    public void rummage$setHoveredSlot(int slotIndex) {
        this.rummage$clientHoveredSlot = slotIndex;
    }

    @Override
    public void rummage$setActivePlayer(Player player) {
        this.rummage$activePlayer = player;
    }

    /**
     * 核心逻辑：每 tick 自动推演翻找进度
     */
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void rummage$tickProgress(CallbackInfo ci) {
        if (!(this.rummage$activePlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int newTarget = -1;

        // 优先级 1：检查玩家鼠标悬停的格子
        if (rummage$clientHoveredSlot >= 0 && rummage$clientHoveredSlot < this.slots.size()) {
            if (rummage$isMasked(this.slots.get(rummage$clientHoveredSlot))) {
                newTarget = rummage$clientHoveredSlot;
            }
        }

        // 优先级 2：如果悬停的格子不需要翻找，从头开始自动找第一个未翻找的格子
        if (newTarget == -1) {
            for (Slot slot : this.slots) {
                if (rummage$isMasked(slot)) {
                    newTarget = slot.index;
                    break;
                }
            }
        }

        // 如果目标发生了切换（包括刚打开 UI 时）
        if (newTarget != rummage$currentTargetSlot) {
            this.rummage$currentTargetSlot = newTarget;
            this.rummage$progress = 0;

            if (newTarget != -1) {
                Slot targetSlot = this.slots.get(newTarget);
                RummageTarget target = CommonContainerUtil.getTarget(targetSlot.container, targetSlot.getContainerSlot());

                if (target != null) {
                    this.rummage$totalTime = target.entity().getRummageTime(targetSlot);
                    // 通知客户端：开始对该格子播放读条动画
                    NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new RummageActionPacket(newTarget, 1, this.rummage$totalTime));
                }
            } else {
                // 通知客户端：停止读条
                NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new RummageActionPacket(-1, 0, 0));
            }
        }

        // 推进进度
        if (this.rummage$currentTargetSlot != -1) {
            Slot targetSlot = this.slots.get(this.rummage$currentTargetSlot);
            RummageTarget target = CommonContainerUtil.getTarget(targetSlot.container, targetSlot.getContainerSlot());

            if (target != null) {
                // --- O(1) 抢占检测：如果已经被别人搜刮完成，直接打断 ---
                if (target.entity().isSlotRummaged(serverPlayer, target.localSlotIndex())) {
                    this.rummage$currentTargetSlot = -1;
                    this.rummage$progress = 0;
                    NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new RummageActionPacket(-1, 0, 0));
                    return;
                }
                // --------------------------------------------------------

                this.rummage$progress++;
                if (this.rummage$progress >= this.rummage$totalTime) {
                    // 翻找完成！执行多人同步解锁与广播逻辑

                    // 1. 软依赖判断：是否为 Lootr 容器
                    String className = target.entity().getClass().getName().toLowerCase(Locale.ROOT);
                    boolean isLootr = className.contains("lootr");

                    // 2. 收集需要同步的玩家 UUID
                    Set<UUID> syncUUIDs = new HashSet<>();
                    if (isLootr) {
                        syncUUIDs.add(serverPlayer.getUUID());
                    } else {
                        syncUUIDs.addAll(target.entity().getRummagingPlayer());
                    }

                    // 3. 遍历并向所有目标玩家下发进度与音效
                    for (UUID uuid : syncUUIDs) {
                        ServerPlayer syncPlayer = serverPlayer.server.getPlayerList().getPlayer(uuid);
                        if (syncPlayer != null) {

                            // 更新目标玩家在服务端的状态
                            target.entity().markSlotRummaged(syncPlayer, target.localSlotIndex());

                            // 独立播放音效，带随机音高
                            SoundEvent sound = target.entity().getRummageCompletedSound(targetSlot);
                            if (sound != null) {
                                float pitch = 1.0F + (syncPlayer.getRandom().nextFloat() - 0.5F) * 0.15F;
                                syncPlayer.playNotifySound(sound, SoundSource.PLAYERS, 0.5F, pitch);
                            }

                            // 寻找对应玩家 UI 里的真实格子索引并发包
                            int targetUiIndex = -1;
                            if (syncPlayer == serverPlayer) {
                                targetUiIndex = this.rummage$currentTargetSlot;
                            } else if (syncPlayer.containerMenu != null) {
                                for (Slot s : syncPlayer.containerMenu.slots) {
                                    RummageTarget t = CommonContainerUtil.getTarget(s.container, s.getContainerSlot());
                                    if (t != null && t.entity() == target.entity() && t.localSlotIndex() == target.localSlotIndex()) {
                                        targetUiIndex = s.index;
                                        break;
                                    }
                                }
                            }

                            if (targetUiIndex != -1) {
                                NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> syncPlayer),
                                        new RummageActionPacket(targetUiIndex, 2, 0));
                            }
                        }
                    }

                    // 将目标置空，下个 tick 会自动寻找下一个格子
                    this.rummage$currentTargetSlot = -1;
                }
            }
        }
    }

    @Unique
    private boolean rummage$isMasked(Slot slot) {
        RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());
        return target != null
                && target.entity().isNeedRummage(this.rummage$activePlayer.getUUID())
                && !target.entity().isSlotRummaged(this.rummage$activePlayer, target.localSlotIndex());
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void rummage$onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        this.rummage$activePlayer = player;

        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());

            if (target != null && !target.entity().isSlotRummaged(player, target.localSlotIndex())) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack rummage$preventQuickStackMerge(Slot slot, Operation<ItemStack> original) {
        if (this.rummage$activePlayer != null) {
            RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());
            if (target != null && !target.entity().isSlotRummaged(this.rummage$activePlayer, target.localSlotIndex())) {
                return ItemStack.EMPTY;
            }
        }
        return original.call(slot);
    }

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean rummage$preventQuickStackPlace(Slot slot, ItemStack stack, Operation<Boolean> original) {
        if (this.rummage$activePlayer != null) {
            RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());
            if (target != null && !target.entity().isSlotRummaged(this.rummage$activePlayer, target.localSlotIndex())) {
                return false;
            }
        }
        return original.call(slot, stack);
    }
}