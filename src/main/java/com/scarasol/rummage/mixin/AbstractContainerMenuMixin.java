package com.scarasol.rummage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scarasol.rummage.api.mixin.IRummageMenu;
import com.scarasol.rummage.compat.sona.SonaCompat;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.RummageActionPacket;
import com.scarasol.rummage.util.CommonContainerUtil;
import com.scarasol.rummage.util.ServerRummageUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    private int rummage$findNextTarget() {
        // 优先级 1：检查玩家鼠标悬停的格子
        if (rummage$clientHoveredSlot >= 0 && rummage$clientHoveredSlot < this.slots.size()) {
            if (rummage$isMasked(this.slots.get(rummage$clientHoveredSlot))) {
                return rummage$clientHoveredSlot;
            }
        }
        // 优先级 2：自动找第一个未翻找的格子
        for (Slot slot : this.slots) {
            if (rummage$isMasked(slot)) {
                return slot.index;
            }
        }
        return -1;
    }

    /**
     * 核心逻辑：每 tick 自动推演翻找进度
     */
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void rummage$tickProgress(CallbackInfo ci) {
        if (!(this.rummage$activePlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int newTarget = rummage$findNextTarget();

        // 1. 状态机：如果目标发生了切换（包括刚打开 UI 时）
        if (newTarget != rummage$currentTargetSlot) {
            this.rummage$currentTargetSlot = newTarget;
            this.rummage$progress = 0;

            if (newTarget != -1) {
                Slot targetSlot = this.slots.get(newTarget);
                RummageTarget target = CommonContainerUtil.getTarget(targetSlot, (AbstractContainerMenu)(Object)this);

                if (target != null) {
                    this.rummage$totalTime = target.entity().getRummageTime(rummage$activePlayer, targetSlot);
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

        // 2. 状态机：推进进度
        if (this.rummage$currentTargetSlot != -1) {
            Slot targetSlot = this.slots.get(this.rummage$currentTargetSlot);
            RummageTarget target = CommonContainerUtil.getTarget(targetSlot, (AbstractContainerMenu)(Object)this);

            if (target != null) {
                // --- O(1) 抢占检测：如果已经被别人搜刮完成，直接打断 ---
                if (target.entity().isSlotRummaged(serverPlayer, target.localSlotIndex())) {
                    this.rummage$currentTargetSlot = -1;
                    this.rummage$progress = 0;
                    NetworkHandler.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new RummageActionPacket(-1, 0, 0));
                    return;
                }

                this.rummage$progress++;

                if (ModList.get().isLoaded("sona") && CommonConfig.SONA_EXPOSURE.get()) {
                    SonaCompat.addEffectInRummaging(serverPlayer, targetSlot.getItem());
                }

                // 3. 状态机：完成翻找
                if (this.rummage$progress >= this.rummage$totalTime) {
                    ServerRummageUtil.handleRummageCompletion(serverPlayer, (AbstractContainerMenu)(Object)this, targetSlot, target);

                    // 将目标置空，下个 tick 会自动寻找下一个格子
                    this.rummage$currentTargetSlot = -1;
                }
            }
        }
    }

    @Unique
    private boolean rummage$isMasked(Slot slot) {
        if (this.rummage$activePlayer == null) {
            return false;
        }

        if (this.rummage$activePlayer.level().isClientSide()) {
            return com.scarasol.rummage.manager.ClientRummageManager.shouldMask(slot.index);
        }

        RummageTarget target = CommonContainerUtil.getTarget(slot, (AbstractContainerMenu)(Object)this);
        return target != null
                && target.entity().isNeedRummage(this.rummage$activePlayer.getUUID())
                && !target.entity().isSlotRummaged(this.rummage$activePlayer, target.localSlotIndex());
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void rummage$onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        this.rummage$activePlayer = player;

        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (this.rummage$isMasked(slot)) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack rummage$preventQuickStackMerge(Slot slot, Operation<ItemStack> original) {
        if (this.rummage$isMasked(slot)) {
            return ItemStack.EMPTY;
        }
        return original.call(slot);
    }

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean rummage$preventQuickStackPlace(Slot slot, ItemStack stack, Operation<Boolean> original) {
        if (this.rummage$isMasked(slot)) {
            return false;
        }
        return original.call(slot, stack);
    }
}