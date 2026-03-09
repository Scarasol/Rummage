package com.scarasol.rummage.api.mixin;

import com.scarasol.rummage.compat.ModCompat;
import com.scarasol.rummage.compat.itemrarity.ItemRarityCompat;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;

import java.util.BitSet;
import java.util.UUID;

public interface IRummageableContainer extends Container, IRummageable {

    default boolean isFullyRummaged(Player player) {
        if (!this.isNeedRummage(player)) {
            return true;
        }
        BitSet progress = this.getRummageProgressByUUID(player);
        int containerSize = this.getContainerSize();
        return progress.cardinality() >= containerSize;
    }

    default void addFullyRummagedPlayer(UUID playerUUID) {
        getFullyRummagedPlayer().add(playerUUID);
        this.setChanged();
    }

    default BitSet initRummageBitSet(Player player) {
        BitSet bitSet = new BitSet();
        int size = this.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                bitSet.set(i);
            } else if (ModCompat.isLoadItemRarity()) {
                if (!ItemRarityCompat.isNeedRummage(player, itemStack)) {
                    bitSet.set(i);
                }
            }
        }
        return bitSet;
    }
}
