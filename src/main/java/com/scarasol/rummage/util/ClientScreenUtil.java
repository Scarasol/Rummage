package com.scarasol.rummage.util;

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
                    Minecraft.getInstance().player.playSound(com.scarasol.rummage.init.RummageSounds.RUMMAGING.get(), 1.0F, 1.0F);
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
        if (ModList.get().isLoaded("petiteinventory") && slot.hasItem()) {
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
}