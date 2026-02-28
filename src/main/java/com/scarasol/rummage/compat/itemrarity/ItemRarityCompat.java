package com.scarasol.rummage.compat.itemrarity;

import com.scarasol.itemrarity.data.RarityGrade;
import com.scarasol.itemrarity.util.RarityGradeUtil;
import com.scarasol.rummage.configuration.CommonConfig;
import net.minecraft.sounds.SoundEvent;
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
    public static SoundEvent getRummagedSoundEventByRarity(Slot slot, SoundEvent originSound) {
        if (slot.hasItem()) {
            String id = getRarityId(slot.getItem());
            SoundEvent soundEvent = CommonConfig.getRummageSound(id);
            return soundEvent == null ? originSound : soundEvent;
        }
        return originSound;
    }
}
