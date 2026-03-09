package com.scarasol.rummage.compat.sona;

import com.scarasol.rummage.compat.ModCompat;
import com.scarasol.rummage.compat.petiteinventory.PetiteInventoryCompat;
import com.scarasol.rummage.init.RummageAttributes;
import com.scarasol.sona.init.SonaMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * @author Scarasol
 */
public class SonaCompat {

    public static void addEffectInRummaging(Player player, ItemStack itemStack) {
        double value = RummageAttributes.getAttributeValue(player, RummageAttributes.SILENT_RUMMAGE.get());
        if (value > 1e-5) {
            return;
        }
        int amplifier = 0;
        if (ModCompat.isLoadPetiteInventory()) {
            int width = PetiteInventoryCompat.getWidth(itemStack);
            int height = PetiteInventoryCompat.getHeight(itemStack);
            amplifier = width * height / 4;
        }
        addEffect(player, amplifier);
    }


    public static void addEffect(Player player, int amplifier) {
        player.addEffect(new MobEffectInstance(SonaMobEffects.EXPOSURE.get(), 10, amplifier, false, false, true));
    }
}
