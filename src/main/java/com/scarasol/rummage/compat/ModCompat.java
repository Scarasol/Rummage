package com.scarasol.rummage.compat;

import net.minecraftforge.fml.ModList;

/**
 * @author Scarasol
 */
public class ModCompat {

    public static boolean isLoadSona() {
        return ModList.get().isLoaded("sona");
    }

    public static boolean isLoadItemRarity() {
        return ModList.get().isLoaded("item_rarity");
    }

    public static boolean isLoadPetiteInventory() {
        return ModList.get().isLoaded("petiteinventory");
    }
}
