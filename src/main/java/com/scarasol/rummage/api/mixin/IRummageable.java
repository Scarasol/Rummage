package com.scarasol.rummage.api.mixin;

import com.scarasol.rummage.compat.ModCompat;
import com.scarasol.rummage.compat.itemrarity.ItemRarityCompat;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.init.RummageAttributes;
import com.scarasol.rummage.init.RummageSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

    BitSet initRummageBitSet(Player player);

    void setNeedRummage(boolean b);

    boolean isInBlackList();

    default void addFullyRummagedPlayer(UUID playerUUID) {
        getFullyRummagedPlayer().add(playerUUID);
    }

    default boolean isNeedRummage(Player player) {
        if (!CommonConfig.CREATIVE_RUMMAGE.get() && player.isCreative()) {
            return false;
        }
        return this.isNeedRummage() && !this.getFullyRummagedPlayer().contains(player.getUUID());
    }

    default BitSet getRummageProgressByUUID(Player player) {

        return this.getRummageProgress().computeIfAbsent(player.getUUID(), k -> initRummageBitSet(player));
    }

    default void removeRummageProgressByUUID(UUID playerUUID) {
        this.getRummageProgress().remove(playerUUID);
    }

    default boolean isSlotRummaged(Player player, int slotIndex) {

        if (!this.isNeedRummage(player)) {
            return true;
        }
        BitSet progress = this.getRummageProgressByUUID(player);
        return progress.get(slotIndex);
    }

    default void markSlotRummaged(Player player, int slotIndex) {
        UUID playerUUID = player.getUUID();
        if (this.isFullyRummaged(player)) {
            return;
        }
        BitSet progress = this.getRummageProgressByUUID(player);
        progress.set(slotIndex);
        if (isFullyRummaged(player)) {
            this.addFullyRummagedPlayer(playerUUID);
            this.removeRummageProgressByUUID(playerUUID);
            this.getRummagingPlayer().remove(playerUUID);
        }
    }

    default int getRummageTime(Player player, Slot slot) {

        double value = RummageAttributes.getAttributeValue(player, RummageAttributes.RUMMAGE_MODIFIER.get());

        if (ModCompat.isLoadItemRarity()) {
            return (int) (ItemRarityCompat.getRummageTimeByRarity(slot, (int) (CommonConfig.RUMMAGE_TIME.get() * 20)) / value);
        }
        return (int) (CommonConfig.RUMMAGE_TIME.get() * 20 / value);
    }


    @Nullable
    default SoundEvent getRummageCompletedSound(Slot slot) {
        if (ModCompat.isLoadItemRarity()) {
            return ItemRarityCompat.getRummagedSoundEventByRarity(slot);
        }
        return CommonConfig.getBaseRummageSound();
    }


    default double getDestroyChance(ItemStack itemStack) {
        if (ModCompat.isLoadItemRarity()) {
            return ItemRarityCompat.getDestroyChanceByRarity(itemStack);
        }
        return CommonConfig.DESTROY_CHANCE.get();
    }


}
