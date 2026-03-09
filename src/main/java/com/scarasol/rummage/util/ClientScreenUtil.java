package com.scarasol.rummage.util;

import com.scarasol.rummage.compat.ModCompat;
import com.scarasol.rummage.compat.petiteinventory.PetiteInventoryCompat;
import com.scarasol.rummage.manager.ClientRummageManager;
import com.scarasol.rummage.network.HoverSlotPacket;
import com.scarasol.rummage.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.ModList;

import static com.scarasol.rummage.RummageMod.MODID;

/**
 * 客户端 UI 渲染与交互工具类
 * @author Scarasol
 */
public class ClientScreenUtil {

    // --- 渲染相关素材 ---
    private static final ResourceLocation SINGLE_RUMMAGE_MASK = new ResourceLocation(MODID, "textures/gui/single_rummage_mask.png");
    private static final ResourceLocation RUMMAGE_MASK = new ResourceLocation(MODID, "textures/gui/rummage_mask.png");
    private static final ResourceLocation RUMMAGE_MASK_FRAME = new ResourceLocation(MODID, "textures/gui/rummage_mask_frame.png");
    private static final ResourceLocation IS_RUMMAGING = new ResourceLocation(MODID, "textures/gui/is_rummaging.png");

    // --- 悬停与音效状态变量 ---
    private static int pendingHoverIndex = -1;
    private static int lastSentHoverIndex = -1;
    private static long hoverStartTime = 0L;
    private static final long HOVER_DELAY_MS = 500L;
    private static long nextPlayTime = 0L;

    /**
     * 处理悬停延迟发包与翻找随机音效
     */
    public static void handleHoverAndSound(Slot actualHovered) {
        int currentIndex = (actualHovered != null) ? actualHovered.index : -1;

        // 1. 悬停状态检测
        if (currentIndex != pendingHoverIndex) {
            pendingHoverIndex = currentIndex;
            hoverStartTime = System.currentTimeMillis();
        }

        // 2. 延迟发送悬停数据包
        if (System.currentTimeMillis() - hoverStartTime >= HOVER_DELAY_MS) {
            if (pendingHoverIndex != lastSentHoverIndex) {
                lastSentHoverIndex = pendingHoverIndex;
                if (pendingHoverIndex == -1 || ClientRummageManager.shouldMask(pendingHoverIndex)) {
                    NetworkHandler.PACKET_HANDLER.sendToServer(new HoverSlotPacket(pendingHoverIndex));
                }
            }
        }

        // 3. 随机翻找音效逻辑
        if (!ClientRummageManager.MASKED_MENU_SLOTS.isEmpty()) {
            long currentTime = System.currentTimeMillis();

            // 如果是第一次进入或者到达了随机的下一次播放时间
            if (nextPlayTime == 0L || currentTime >= nextPlayTime) {
                // 播放翻找音效
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(com.scarasol.rummage.init.RummageSounds.RUMMAGING_DELTAFORCE.get(), 1.0F, 1.0F);
                }
                // 设置下一次播放的时间：5000ms - 7000ms 之间的随机值
                long delay = 5000L + (long)(Math.random() * 2000L);
                nextPlayTime = currentTime + delay;
            }
        } else {
            // 当所有格子都翻完了，重置计时器
            nextPlayTime = 0L;
        }
    }

    /**
     * 渲染 Rummage 的遮罩、边框和动画
     */
    public static void renderRummageMask(GuiGraphics graphics, Slot slot, AbstractContainerMenu menu) {
        // 默认尺寸为 16x16
        int pixelW = 16;
        int pixelH = 16;

        // 1. 计算大物品的实际像素尺寸
        if (ModCompat.isLoadPetiteInventory() && slot.hasItem()) {
            if (PetiteInventoryCompat.isMenuEnabled(menu)) {
                int pWidth = PetiteInventoryCompat.getWidth(slot.getItem());
                int pHeight = PetiteInventoryCompat.getHeight(slot.getItem());

                if (pWidth > 1 || pHeight > 1) {
                    pixelW = 16 + (pWidth - 1) * 18;
                    pixelH = 16 + (pHeight - 1) * 18;
                }
            }
        }

        // 2. 根据尺寸决定渲染策略
        if (pixelW == 16 && pixelH == 16) {
            // --- 单格物品渲染 (性能优化) ---
            // 直接渲染 16x16 的单格遮罩，无需计算边框偏移和拼接
            graphics.blit(SINGLE_RUMMAGE_MASK, slot.x, slot.y, 0.0f, 0.0f, 16, 16, 16, 16);
        } else {
            // --- 大物品拼接渲染 ---
            int textureWidth = 162;
            int textureHeight = 108;

            // 阶段 1: 渲染外部边框 (向左上角偏移1像素，终点不变)
            graphics.blit(RUMMAGE_MASK_FRAME, slot.x - 1, slot.y - 1, 0.0f, 0.0f, pixelW + 1, pixelH + 1, textureWidth, textureHeight);

            // 阶段 2: 渲染内部遮罩 (配合边框向内缩小，留出1像素边缘)
            graphics.blit(RUMMAGE_MASK, slot.x, slot.y, 1.0f, 1.0f, pixelW - 1, pixelH - 1, textureWidth, textureHeight);
        }

        // --- 3. 如果正在翻找，叠加渲染居中的动画图标 ---
        if (slot.index == ClientRummageManager.currentRummageSlot) {
            int frame = (int) ((System.currentTimeMillis() / 100L) % 12);
            int vOffset = frame * 16;

            // 计算大物品区域的中心点 (对于单格物品，这里刚好是中心)
            int centerX = slot.x + (pixelW / 2) - 8;
            int centerY = slot.y + (pixelH / 2) - 8;

            graphics.blit(IS_RUMMAGING, centerX, centerY, 0, vOffset, 16, 16, 16, 192);
        }
    }

    // 替换 ClientScreenUtil.java 中的 renderFlashHighlight 方法
    public static void renderFlashHighlight(GuiGraphics graphics, Slot slot, AbstractContainerMenu menu) {
        long startTime = ClientRummageManager.RECENTLY_UNMASKED_SLOTS.getOrDefault(slot.index, 0L);
        if (startTime == 0L) return;

        long elapsed = System.currentTimeMillis() - startTime;

        // ==========================================
        // === 闪烁动画参数设置区（你可以随时在这里微调） ===
        // ==========================================

        // 1. 半周期时间 (毫秒)：从最暗到最亮（或从最亮到最暗）所需的时间。
        // 300L 意味着一个完整的“亮起+熄灭”的呼吸周期是 600ms。
        long halfCycleMs = 200L;

        // 2. 闪烁总次数：控制这个格子被连锁出来后，一共要呼吸闪烁几次才停止。
        int flashCount = 2;

        // 3. 最大透明度 (0-255)：128 大约是 50% 的透明度。可以根据材质亮度微调。
        int maxAlpha = 128;

        // ==========================================

        // 自动计算完整周期和总时长
        long cycleMs = halfCycleMs * 2;
        long flashDuration = cycleMs * flashCount;

        // 超过总时长则移除闪烁状态
        if (elapsed > flashDuration) {
            ClientRummageManager.RECENTLY_UNMASKED_SLOTS.remove(slot.index);
            return;
        }

        // === 平滑透明度计算核心逻辑 ===
        // 按照当前的 cycleMs 动态计算正弦映射
        double radians = ((elapsed % cycleMs) / (double) cycleMs) * 2 * Math.PI - (Math.PI / 2);
        float alphaProgress = (float) ((Math.sin(radians) + 1.0) / 2.0);

        int currentAlpha = (int) (maxAlpha * alphaProgress);

        // 如果透明度极低，为了节省性能直接跳过渲染
        if (currentAlpha <= 5) return;

        // 将算出的 Alpha 值组合成 ARGB 颜色
        int color = (currentAlpha << 24) | 0xFFFFFF;

        int pixelW = 16;
        int pixelH = 16;

        // 兼容 PetiteInventory 大格子尺寸
        if (ModCompat.isLoadPetiteInventory() && slot.hasItem()) {
            if (PetiteInventoryCompat.isMenuEnabled(menu)) {
                int pWidth = PetiteInventoryCompat.getWidth(slot.getItem());
                int pHeight = PetiteInventoryCompat.getHeight(slot.getItem());

                if (pWidth > 1 || pHeight > 1) {
                    pixelW = 16 + (pWidth - 1) * 18;
                    pixelH = 16 + (pHeight - 1) * 18;
                }
            }
        }

        graphics.pose().pushPose();
        // 提升 Z 轴，确保高亮渲染在物品和数量上方
        graphics.pose().translate(0, 0, 200F);

        // 渲染平滑渐变的白色高亮
        graphics.fill(slot.x, slot.y, slot.x + pixelW, slot.y + pixelH, color);

        graphics.pose().popPose();
    }
}