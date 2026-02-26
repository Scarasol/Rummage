package com.scarasol.rummage.event;

import com.scarasol.rummage.manager.ClientRummageManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author Scarasol
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen) {
            ClientRummageManager.clear();
        }
    }
}
