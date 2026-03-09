package com.scarasol.rummage.manager;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Scarasol
 */
public class ClientRummageManager {

    // 存储当前 UI 中需要遮挡的全局格子索引 (slot.index)
    public static final BitSet MASKED_MENU_SLOTS = new BitSet();

    // 记录被连锁翻找出的格子及其解开的时间戳
    public static final Map<Integer, Long> RECENTLY_UNMASKED_SLOTS = new ConcurrentHashMap<>();

    // --- 动画与状态机变量 ---
    public static int currentRummageSlot = -1;
    public static long rummageStartTime = 0L;
    public static long rummageTotalTimeMs = 0L;

    /**
     * 清空所有客户端状态。
     */
    public static void clear() {
        MASKED_MENU_SLOTS.clear();
        RECENTLY_UNMASKED_SLOTS.clear();
        currentRummageSlot = -1;
        rummageStartTime = 0L;
        rummageTotalTimeMs = 0L;
    }

    public static boolean shouldMask(int menuSlotIndex) {
        return MASKED_MENU_SLOTS.get(menuSlotIndex);
    }
}