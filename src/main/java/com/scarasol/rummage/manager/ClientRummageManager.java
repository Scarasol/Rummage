package com.scarasol.rummage.manager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.BitSet;

/**
 * @author Scarasol
 */
@OnlyIn(Dist.CLIENT)
public class ClientRummageManager {

    // 存储当前 UI 中需要遮挡的全局格子索引 (slot.index)
    public static final BitSet MASKED_MENU_SLOTS = new BitSet();

    // --- 动画与状态机变量 ---
    // 当前正在被搜刮的格子索引，-1 表示没有在搜刮
    public static int currentRummageSlot = -1;
    // 搜刮开始的系统时间戳 (毫秒)，用于客户端补间动画
    public static long rummageStartTime = 0L;
    // 搜刮所需的总时间 (毫秒)
    public static long rummageTotalTimeMs = 0L;

    /**
     * 清空所有客户端状态。
     * 在 UI 关闭，或切换容器时调用。
     */
    public static void clear() {
        MASKED_MENU_SLOTS.clear();
        currentRummageSlot = -1;
        rummageStartTime = 0L;
        rummageTotalTimeMs = 0L;
    }

    /**
     * 判断特定的格子是否需要被遮罩
     */
    public static boolean shouldMask(int menuSlotIndex) {
        return MASKED_MENU_SLOTS.get(menuSlotIndex);
    }
}