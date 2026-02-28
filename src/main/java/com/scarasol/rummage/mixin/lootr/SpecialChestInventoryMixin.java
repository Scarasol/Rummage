package com.scarasol.rummage.mixin.lootr;

import com.scarasol.rummage.api.mixin.ILootrInventoryDelegate;
import com.scarasol.rummage.api.mixin.IRummageableContainerEntity;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import net.minecraft.sounds.SoundEvent;

import net.minecraft.world.inventory.Slot;
import noobanidus.mods.lootr.data.SpecialChestInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(value = SpecialChestInventory.class, remap = false)
public abstract class SpecialChestInventoryMixin implements IRummageableContainerEntity, ILootrInventoryDelegate {

    @Unique
    private IRummageableEntity rummage$blockEntityDelegate;

    @Override
    public void rummage$setBlockEntity(IRummageableEntity entity) {
        this.rummage$blockEntityDelegate = entity;
    }

    @Override
    public IRummageableEntity rummage$getBlockEntity() {
        return this.rummage$blockEntityDelegate;
    }

    @Override
    public Set<UUID> getFullyRummagedPlayer() {
        return rummage$blockEntityDelegate != null ? rummage$blockEntityDelegate.getFullyRummagedPlayer() : new HashSet<>();
    }

    @Override
    public boolean isNeedRummage() {
        return rummage$blockEntityDelegate != null && rummage$blockEntityDelegate.isNeedRummage();
    }

    @Override
    public Set<UUID> getRummagingPlayer() {
        return rummage$blockEntityDelegate != null ? rummage$blockEntityDelegate.getRummagingPlayer() : new HashSet<>();
    }

    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return rummage$blockEntityDelegate != null ? rummage$blockEntityDelegate.getRummageProgress() : new HashMap<>();
    }

    @Override
    public UUID getUUID() {
        return rummage$blockEntityDelegate != null ? rummage$blockEntityDelegate.getUUID() : UUID.randomUUID();
    }

    @Override
    public SoundEvent getRummageCompletedSound(Slot slot) {
        return rummage$blockEntityDelegate != null ? rummage$blockEntityDelegate.getRummageCompletedSound(slot) : null;
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