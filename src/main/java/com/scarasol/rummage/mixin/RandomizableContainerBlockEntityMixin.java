package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableContainerEntity;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;


import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Scarasol
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin extends BaseContainerBlockEntity implements IRummageableContainerEntity {

    @Shadow
    @Nullable
    protected ResourceLocation lootTable;
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

    public RandomizableContainerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    private void rummage$setLootTable(ResourceLocation lootTable, long seed, CallbackInfo ci) {
        rummage$entityUUID = UUID.randomUUID();
        rummage$needRummage = true;
    }

    @Override
    public SoundEvent getRummageCompletedSound(Slot slot) {
        return null;
    }

    /**
     * 重写 Forge 的能力获取方法。
     * 当任何机器（漏斗、模组管道）试图连接这个箱子时，都会调用这个方法。
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {

        if (cap == ForgeCapabilities.ITEM_HANDLER && this.rummage$needRummage) {
            if (this.rummage$blockedHandler == null || !this.rummage$blockedHandler.isPresent()) {
                this.rummage$blockedHandler = LazyOptional.of(() -> new IItemHandler() {
                    @Override public int getSlots() { return 0; }
                    @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
                    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; } // 原路弹回，拒绝注入
                    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; } // 抽不出任何东西
                    @Override public int getSlotLimit(int slot) { return 0; }
                    @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
                });
            }
            return this.rummage$blockedHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (this.rummage$blockedHandler != null) {
            this.rummage$blockedHandler.invalidate();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.rummage$needRummage && this.lootTable == null && this.isEmpty()) {
            ResourceLocation typeId = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(this.getType());
            boolean isLootr = typeId != null && "lootr".equals(typeId.getNamespace());
            if (!isLootr) {
                this.rummage$needRummage = false;
                if (this.rummage$blockedHandler != null) {
                    this.rummage$blockedHandler.invalidate();
                }
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (rummage$entityUUID != null) {
            compoundTag.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, rummage$needRummage);
            compoundTag.putUUID(CommonContainerUtil.RUMMAGE_UUID_KEY, rummage$entityUUID);
            CommonContainerUtil.savePlayList(compoundTag, rummage$fullyRummagedPlayer);
        }

    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        if (compoundTag.contains(CommonContainerUtil.RUMMAGE_UUID_KEY)) {
            rummage$entityUUID = compoundTag.getUUID(CommonContainerUtil.RUMMAGE_UUID_KEY);
            rummage$needRummage = compoundTag.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compoundTag));
        } else if (compoundTag.contains("LootTable")){
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
