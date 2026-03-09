package com.scarasol.rummage.compat.itemrarity;

import com.scarasol.itemrarity.data.RarityGrade;
import com.scarasol.itemrarity.util.RarityGradeUtil;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.init.RummageAttributes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * @author Scarasol
 */
public class ItemRarityCompat {


    public static String getRarityId(ItemStack itemStack) {
        RarityGrade rarityGrade = RarityGradeUtil.getRarityGrade(itemStack);
        if (rarityGrade != null) {
            return rarityGrade.getId();
        }
        return "";
    }

    public static int getRummageTimeByRarity(Slot slot, int originTime) {
        if (slot.hasItem()) {
            String id = getRarityId(slot.getItem());
            return (int) (CommonConfig.getRummageTime(id) * 20);
        }
        return originTime;
    }

    @Nullable
    public static SoundEvent getRummagedSoundEventByRarity(Slot slot) {
        if (slot.hasItem()) {
            String id = getRarityId(slot.getItem());

            return CommonConfig.getRummageSound(id);
        }
        return CommonConfig.getBaseRummageSound();
    }

    public static double getDestroyChanceByRarity(ItemStack itemStack) {
        String id = getRarityId(itemStack);
        return CommonConfig.getDestroyChance(id);
    }

    public static boolean isNeedRummage(Player player, ItemStack itemStack) {
        RarityGrade rarityGrade = RarityGradeUtil.getRarityGrade(itemStack);
        double value = RummageAttributes.getAttributeValue(player, RummageAttributes.MIN_RUMMAGE_RARITY.get());
        if (rarityGrade != null) {
            return rarityGrade.getGrade() >= value;
        }
        return value < 1e-5;
    }
}
