package com.scarasol.rummage.api.mixin;

import com.scarasol.rummage.compat.itemrarity.ItemRarityCompat;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.init.RummageSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Scarasol
 */
public interface IRummageable {

    Set<UUID> getFullyRummagedPlayer();

    boolean isNeedRummage();

    Set<UUID> getRummagingPlayer();

    boolean isFullyRummaged(Player player);

    Map<UUID, BitSet> getRummageProgress();

    UUID getRummageableUUID();

    BitSet initRummageBitSet();

    void setNeedRummage(boolean b);

    boolean isInBlackList();

    default void addFullyRummagedPlayer(UUID playerUUID) {
        getFullyRummagedPlayer().add(playerUUID);
    }

    default boolean isNeedRummage(UUID playerUUID) {
        return this.isNeedRummage() && !this.getFullyRummagedPlayer().contains(playerUUID);
    }

    default BitSet getRummageProgressByUUID(UUID playerUUID) {
        return this.getRummageProgress().computeIfAbsent(playerUUID, k -> initRummageBitSet());
    }

    default void removeRummageProgressByUUID(UUID playerUUID) {
        this.getRummageProgress().remove(playerUUID);
    }

    default boolean isSlotRummaged(Player player, int slotIndex) {
        UUID playerUUID = player.getUUID();
        if (!this.isNeedRummage(playerUUID)) {
            return true;
        }
        BitSet progress = this.getRummageProgressByUUID(playerUUID);
        return progress.get(slotIndex);
    }

    default void markSlotRummaged(Player player, int slotIndex) {
        UUID playerUUID = player.getUUID();
        if (this.isFullyRummaged(player)) {
            return;
        }
        BitSet progress = this.getRummageProgressByUUID(playerUUID);
        progress.set(slotIndex);
        if (isFullyRummaged(player)) {
            this.addFullyRummagedPlayer(playerUUID);
            this.removeRummageProgressByUUID(playerUUID);
            this.getRummagingPlayer().remove(playerUUID);
        }
    }

    default int getRummageTime(Slot slot) {
        if (ModList.get().isLoaded("item_rarity")) {
            return ItemRarityCompat.getRummageTimeByRarity(slot, (int) (CommonConfig.RUMMAGE_TIME.get() * 20));
        }
        return (int) (CommonConfig.RUMMAGE_TIME.get() * 20);
    }


    @Nullable
    default SoundEvent getRummageCompletedSound(Slot slot) {
        if (ModList.get().isLoaded("item_rarity")) {
            return ItemRarityCompat.getRummagedSoundEventByRarity(slot);
        }
        return CommonConfig.getBaseRummageSound();
    }


    default double getDestroyChance(ItemStack itemStack) {
        if (ModList.get().isLoaded("item_rarity")) {
            return ItemRarityCompat.getDestroyChanceByRarity(itemStack);
        }
        return CommonConfig.DESTROY_CHANCE.get();
    }


}
