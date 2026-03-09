package com.scarasol.rummage.mixin.corpse;

import com.scarasol.rummage.api.mixin.ICorpseInventoryDelegate;
import com.scarasol.rummage.api.mixin.ICorpseRummageable;
import com.scarasol.rummage.api.mixin.IRummageable;
import com.scarasol.rummage.api.mixin.IRummageableContainer;
import de.maxhenkel.corpse.corelib.inventory.ItemListInventory;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(value = ItemListInventory.class, remap = false)
public abstract class ItemListInventoryMixin implements IRummageableContainer, ICorpseInventoryDelegate {

    @Unique private IRummageable rummage$corpseDelegate;
    @Unique private int rummage$inventoryType;

    @Override
    public void rummage$setCorpse(IRummageable corpse, int type) {
        this.rummage$corpseDelegate = corpse;
        this.rummage$inventoryType = type;
    }

    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        if (rummage$corpseDelegate instanceof ICorpseRummageable multi) {
            return multi.rummage$getProgress(rummage$inventoryType);
        }
        return new HashMap<>();
    }

    @Override
    public void removeRummageProgressByUUID(UUID playerUUID) {
        if (rummage$corpseDelegate != null) {
            rummage$corpseDelegate.removeRummageProgressByUUID(playerUUID);
        }
    }

    // =============== 原生委托区 (不夹带任何私货) ===============

    @Override
    public boolean isNeedRummage(Player player) {
        return rummage$corpseDelegate != null && rummage$corpseDelegate.isNeedRummage(player);
    }

    @Override
    public UUID getRummageableUUID() {
        return rummage$corpseDelegate != null ? rummage$corpseDelegate.getRummageableUUID() : UUID.randomUUID();
    }

    @Override
    public boolean isFullyRummaged(Player player) {
        return rummage$corpseDelegate != null && rummage$corpseDelegate.isFullyRummaged(player);
    }

    @Override
    public void setNeedRummage(boolean needRummage) {
        if (rummage$corpseDelegate != null) {
            rummage$corpseDelegate.setNeedRummage(needRummage);
        }
    }

    @Override
    public boolean isNeedRummage() {
        return rummage$corpseDelegate != null && rummage$corpseDelegate.isNeedRummage();
    }

    @Override
    public Set<UUID> getFullyRummagedPlayer() {
        return rummage$corpseDelegate != null ? rummage$corpseDelegate.getFullyRummagedPlayer() : new HashSet<>();
    }

    @Override
    public Set<UUID> getRummagingPlayer() {
        return rummage$corpseDelegate != null ? rummage$corpseDelegate.getRummagingPlayer() : new HashSet<>();
    }

    @Override
    public SoundEvent getRummageCompletedSound(Slot slot) {
        return rummage$corpseDelegate != null ? rummage$corpseDelegate.getRummageCompletedSound(slot) : null;
    }

    @Override
    public int getRummageTime(Player player, Slot slot) {
        return rummage$corpseDelegate != null ? rummage$corpseDelegate.getRummageTime(player, slot) : 0;
    }
}