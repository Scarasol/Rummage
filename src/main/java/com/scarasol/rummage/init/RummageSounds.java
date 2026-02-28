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


    public static final RegistryObject<SoundEvent> RUMMAGING = register("rummaging");


    public static final RegistryObject<SoundEvent> NORMAL_FOUND = register("normal_found");
    public static final RegistryObject<SoundEvent> UNCOMMON_FOUND = register("uncommon_found");
    public static final RegistryObject<SoundEvent> RARE_FOUND = register("rare_found");
    public static final RegistryObject<SoundEvent> EPIC_FOUND = register("epic_found");
    public static final RegistryObject<SoundEvent> LEGENDARY_FOUND = register("legendary_found");
    public static final RegistryObject<SoundEvent> ARTIFACT_FOUND = register("artifact_found");


    public static final RegistryObject<SoundEvent> BACK_UP = register("back_up");


    private static RegistryObject<SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RummageMod.MODID, name)));
    }
}