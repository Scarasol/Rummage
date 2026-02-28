package com.scarasol.rummage;

import com.mojang.logging.LogUtils;

import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.init.RummageSounds;
import com.scarasol.rummage.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * @author Scarasol
 */
@Mod(RummageMod.MODID)
public class RummageMod
{

    public static final String MODID = "rummage";

    public static final Logger LOGGER = LogUtils.getLogger();

    public RummageMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "rummage-common.toml");
        RummageSounds.REGISTRY.register(modEventBus);
        NetworkHandler.addNetworkMessage();
    }


}
