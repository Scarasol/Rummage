package com.scarasol.rummage.init;

import com.scarasol.rummage.RummageMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * @author Scarasol
 */
@Mod.EventBusSubscriber(modid = RummageMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RummageAttributes {
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RummageMod.MODID);

    public static final RegistryObject<Attribute> RUMMAGE_MODIFIER = REGISTRY.register("rummage_modifier",
            () -> new RangedAttribute("attribute.rummage.rummage_modifier", 1.0D, 0.0D, 1024.0D));

    public static final RegistryObject<Attribute> CAN_CHAIN_RUMMAGE = REGISTRY.register("can_chain_rummage",
            () -> new RangedAttribute("attribute.rummage.can_chain_rummage", 1.0D, 0.0D, 0.0D));

    public static final RegistryObject<Attribute> SILENT_RUMMAGE = REGISTRY.register("silent_rummage",
            () -> new RangedAttribute("attribute.rummage.silent_rummage", 0.0D, 0.0D, 1.0D));

    public static final RegistryObject<Attribute> MIN_RUMMAGE_RARITY = REGISTRY.register("min_rummage_rarity",
            () -> new RangedAttribute("attribute.rummage.min_rummage_rarity", 0.0D, 0.0D, 1024.0D));

    public static final RegistryObject<Attribute> DESTROY_CHANCE = REGISTRY.register("destroy_chance",
            () -> new RangedAttribute("attribute.rummage.destroy_chance", 1.0D, 0.0D, 1024.0D));

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, RummageAttributes.RUMMAGE_MODIFIER.get());
        event.add(EntityType.PLAYER, RummageAttributes.CAN_CHAIN_RUMMAGE.get());
        event.add(EntityType.PLAYER, RummageAttributes.SILENT_RUMMAGE.get());
        event.add(EntityType.PLAYER, RummageAttributes.MIN_RUMMAGE_RARITY.get());
        event.add(EntityType.PLAYER, RummageAttributes.DESTROY_CHANCE.get());
    }

    public static double getAttributeValue(Player player, Attribute attribute) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            return attributeInstance.getValue();
        }
        return attribute.getDefaultValue();
    }
}