package com.scarasol.rummage.network;

import com.scarasol.rummage.RummageMod;
import com.scarasol.rummage.manager.ClientRummageManager;

/**
 * @author Scarasol
 */
public class ClientPacketHandler {

    public static void handle(SyncRummageStatePacket msg) {
        ClientRummageManager.MASKED_MENU_SLOTS.clear();
        ClientRummageManager.MASKED_MENU_SLOTS.or(msg.maskedMenuSlots());
    }

    public static void handleAction(RummageActionPacket msg) {
        if (msg.action() == 2) {
            ClientRummageManager.MASKED_MENU_SLOTS.clear(msg.slotIndex());
            if (ClientRummageManager.currentRummageSlot == msg.slotIndex()) {
                ClientRummageManager.currentRummageSlot = -1;
            }
        } else if (msg.action() == 1) {
            ClientRummageManager.currentRummageSlot = msg.slotIndex();
            ClientRummageManager.rummageStartTime = System.currentTimeMillis();
            ClientRummageManager.rummageTotalTimeMs = msg.totalTicks() * 50L;
        } else if (msg.action() == 0) {
            ClientRummageManager.currentRummageSlot = -1;
        }
    }
}