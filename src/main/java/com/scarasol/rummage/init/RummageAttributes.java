package com.scarasol.rummage.init;

import com.scarasol.rummage.RummageMod;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


/**
 * @author Administrator
 */
public class RummageAttributes {
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RummageMod.MODID);

    public static final RegistryObject<Attribute> RUMMAGE_MODIFIER = REGISTRY.register("rummage_modifier",
            () -> new RangedAttribute(
                    "attribute.rummage.rummage_modifier",
                    1.0D,
                    0.0D,
                    1024.0D
            ).setSyncable(true));
}
