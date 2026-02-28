package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableContainerEntity;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 兼容原版带有物品栏的实体（如运输矿车、漏斗矿车等）
 * @author Scarasol
 */
@Mixin(AbstractMinecartContainer.class)
public abstract class AbstractMinecartContainerMixin implements IRummageableContainerEntity {

    @Shadow
    @Nullable
    private ResourceLocation lootTable;

    @Unique
    private final Set<UUID> rummage$fullyRummagedPlayer = new HashSet<>();

    @Unique
    private final Map<UUID, BitSet> rummage$rummageProgress = new HashMap<>();

    @Unique
    private final Set<UUID> rummage$rummagingPlayer = new HashSet<>();

    @Unique
    private boolean rummage$needRummage;

    @Unique
    private UUID rummage$entityUUID;

    @Unique
    private LazyOptional<IItemHandler> rummage$blockedHandler;

    @Inject(method = "setLootTable", at = @At("TAIL"))
    private void rummage$setLootTable(ResourceLocation lootTable, long seed, CallbackInfo ci) {
        rummage$entityUUID = UUID.randomUUID();
        rummage$needRummage = true;
    }



//    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", at = @At("HEAD"), cancellable = true, remap = false)
//    private <T> void rummage$interceptCapability(Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
//        if (cap == ForgeCapabilities.ITEM_HANDLER && this.rummage$needRummage) {
//            if (this.rummage$blockedHandler == null || !this.rummage$blockedHandler.isPresent()) {
//                this.rummage$blockedHandler = LazyOptional.of(() -> new IItemHandler() {
//                    @Override public int getSlots() { return 0; }
//                    @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
//                    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
//                    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
//                    @Override public int getSlotLimit(int slot) { return 0; }
//                    @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
//                });
//            }
//            cir.setReturnValue(this.rummage$blockedHandler.cast());
//        }
//    }


    @Inject(method = "invalidateCaps", at = @At("TAIL"), remap = false)
    private void rummage$invalidateCaps(CallbackInfo ci) {
        if (this.rummage$blockedHandler != null) {
            this.rummage$blockedHandler.invalidate();
        }
    }

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void rummage$setChanged(CallbackInfo ci) {
        AbstractMinecartContainer container = (AbstractMinecartContainer) (Object) this;
        // 实体矿车没有 Lootr 那些复杂的 typeId 命名空间，所以只要被清空就可以移除搜刮状态
        if (this.rummage$needRummage && this.lootTable == null && container.isEmpty()) {
            this.rummage$needRummage = false;
            if (this.rummage$blockedHandler != null) {
                this.rummage$blockedHandler.invalidate();
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rummage$addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        if (rummage$entityUUID != null) {
            compoundTag.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, rummage$needRummage);
            compoundTag.putUUID(CommonContainerUtil.RUMMAGE_UUID_KEY, rummage$entityUUID);
            CommonContainerUtil.savePlayList(compoundTag, rummage$fullyRummagedPlayer);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rummage$readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        if (compoundTag.contains(CommonContainerUtil.RUMMAGE_UUID_KEY)) {
            rummage$entityUUID = compoundTag.getUUID(CommonContainerUtil.RUMMAGE_UUID_KEY);
            rummage$needRummage = compoundTag.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compoundTag));
        } else if (compoundTag.contains("LootTable")) {
            rummage$entityUUID = UUID.randomUUID();
            rummage$needRummage = true;
        }
    }

    @Override
    public Set<UUID> getFullyRummagedPlayer() {
        return rummage$fullyRummagedPlayer;
    }

    @Override
    public boolean isNeedRummage() {
        return rummage$needRummage;
    }

    @Override
    public Set<UUID> getRummagingPlayer() {
        return rummage$rummagingPlayer;
    }

    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return rummage$rummageProgress;
    }

    @Override
    public UUID getUUID() {
        return rummage$entityUUID;
    }

    @Override
    public BitSet initRummageBitSet() {
        BitSet bitSet = new BitSet();
        int size = this.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (this.getItem(i).isEmpty()) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }
}