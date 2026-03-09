package com.scarasol.rummage.network;

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
            // 1. 判断并记录：如果当前解开的格子不是正在主翻找的格子，则加入闪烁队列
            if (msg.slotIndex() != ClientRummageManager.currentRummageSlot) {
                ClientRummageManager.RECENTLY_UNMASKED_SLOTS.put(msg.slotIndex(), System.currentTimeMillis());
            }

            // 2. 解除遮罩
            ClientRummageManager.MASKED_MENU_SLOTS.clear(msg.slotIndex());

            // 3. 如果解开的是主格子，重置主翻找状态
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