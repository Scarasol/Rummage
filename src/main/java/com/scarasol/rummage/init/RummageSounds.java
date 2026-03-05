package com.scarasol.rummage.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.scarasol.rummage.RummageMod;

/**
 * @author Scarasol
 */
public class RummageSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RummageMod.MODID);

    // Rummaging sounds
    public static final RegistryObject<SoundEvent> RUMMAGING_DELTAFORCE = register("rummaging_deltaforce");

    // Deltaforce series
    public static final RegistryObject<SoundEvent> NORMAL_DELTAFORCE = register("normal_deltaforce");
    public static final RegistryObject<SoundEvent> UNCOMMON_DELTAFORCE = register("uncommon_deltaforce");
    public static final RegistryObject<SoundEvent> RARE_DELTAFORCE = register("rare_deltaforce");
    public static final RegistryObject<SoundEvent> EPIC_DELTAFORCE = register("epic_deltaforce");
    public static final RegistryObject<SoundEvent> LEGENDARY_DELTAFORCE = register("legendary_deltaforce");
    public static final RegistryObject<SoundEvent> ARTIFACT_DELTAFORCE = register("artifact_deltaforce");

    // Nico series
    public static final RegistryObject<SoundEvent> NORMAL_NICO = register("normal_nico");
    public static final RegistryObject<SoundEvent> UNCOMMON_NICO = register("uncommon_nico");
    public static final RegistryObject<SoundEvent> RARE_NICO = register("rare_nico");
    public static final RegistryObject<SoundEvent> EPIC_NICO = register("epic_nico");
    public static final RegistryObject<SoundEvent> LEGENDARY_NICO = register("legendary_nico");
    public static final RegistryObject<SoundEvent> ARTIFACT_NICO = register("artifact_nico");

    // Misc
    public static final RegistryObject<SoundEvent> BACK_UP = register("back_up");

    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RummageMod.MODID, name)));
    }
}