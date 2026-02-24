package com.scarasol.rummage;

import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * @author Scarasol
 */
@Mod(RummageMod.MODID)
public class RummageMod
{

    public static final String MODID = "rummage";

    private static final Logger LOGGER = LogUtils.getLogger();

    public RummageMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


    }


}
