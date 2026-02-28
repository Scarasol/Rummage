package com.scarasol.rummage.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.scarasol.rummage.compat.petiteinventory.PetiteInventoryCompat;
import com.scarasol.rummage.manager.ClientRummageManager;
import com.scarasol.rummage.network.HoverSlotPacket;
import com.scarasol.rummage.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.scarasol.rummage.RummageMod.MODID;

/**
 * @author Scarasol
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Unique
    private static final ResourceLocation RUMMAGE_MASK = new ResourceLocation(MODID, "textures/gui/rummage_mask.png");

    @Unique
    private static final ResourceLocation IS_RUMMAGING = new ResourceLocation(MODID, "textures/gui/is_rummaging.png");

    @Unique
    private int rummage$pendingHoverIndex = -1;
    @Unique
    private int rummage$lastSentHoverIndex = -1;
    @Unique
    private long rummage$hoverStartTime = 0L;
    @Unique
    private static final long HOVER_DELAY_MS = 500L;

    @Unique
    private long rummage$nextPlayTime = 0L;

    @Unique
    private Slot rummage$getActualSlot(@Nullable Slot original) {
        if (original != null && ModList.get().isLoaded("petiteinventory")) {
            if (PetiteInventoryCompat.isMenuEnabled(this.menu)) {
                return PetiteInventoryCompat.getMappedSlot(original);
            }
        }
        return original;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void rummage$detectHoverChange(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Slot actualHovered = rummage$getActualSlot(this.hoveredSlot);
        int currentIndex = (actualHovered != null) ? actualHovered.index : -1;

        if (currentIndex != rummage$pendingHoverIndex) {
            rummage$pendingHoverIndex = currentIndex;
            rummage$hoverStartTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - rummage$hoverStartTime >= HOVER_DELAY_MS) {
            if (rummage$pendingHoverIndex != rummage$lastSentHoverIndex) {
                rummage$lastSentHoverIndex = rummage$pendingHoverIndex;
                if (rummage$pendingHoverIndex == -1 || ClientRummageManager.shouldMask(rummage$pendingHoverIndex)) {
                    NetworkHandler.PACKET_HANDLER.sendToServer(new HoverSlotPacket(rummage$pendingHoverIndex));
                }
            }
        }

        if (!ClientRummageManager.MASKED_MENU_SLOTS.isEmpty()) {
            long currentTime = System.currentTimeMillis();

            // 如果是第一次进入或者到达了随机的下一次播放时间
            if (rummage$nextPlayTime == 0L || currentTime >= rummage$nextPlayTime) {

                // 播放翻找音效
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(com.scarasol.rummage.init.RummageSounds.RUMMAGING.get(), 1.0F, 1.0F);
                }

                // 设置下一次播放的时间：5000ms - 7000ms 之间的随机值
                long delay = 5000L + (long)(Math.random() * 2000L);
                rummage$nextPlayTime = currentTime + delay;
            }
        } else {
            // 当所有格子都翻完了（BitSet 为空），重置计时器
            // 这样下次玩家打开一个新容器并开始翻找时，会立即播放第一声
            rummage$nextPlayTime = 0L;
        }
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void rummage$renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (ClientRummageManager.shouldMask(slot.index)) {

            // 默认尺寸为 16x16
            int pixelW = 16;
            int pixelH = 16;

            // 1. 计算大物品的实际像素尺寸
            if (ModList.get().isLoaded("petiteinventory") && slot.hasItem()) {
                if (PetiteInventoryCompat.isMenuEnabled(this.menu)) {
                    int pWidth = PetiteInventoryCompat.getWidth(slot.getItem());
                    int pHeight = PetiteInventoryCompat.getHeight(slot.getItem());

                    if (pWidth > 1 || pHeight > 1) {
                        pixelW = 16 + (pWidth - 1) * 18;
                        pixelH = 16 + (pHeight - 1) * 18;
                    }
                }
            }

            float scaleX = (float) pixelW / 16.0f;
            float scaleY = (float) pixelH / 16.0f;

            PoseStack poseStack = graphics.pose();

            // --- 阶段 1: 渲染拉伸的静态底图 ---
            poseStack.pushPose();
            // 平移到格子左上角
            poseStack.translate(slot.x, slot.y, 0);

            // 如果是大物品，应用缩放
            if (scaleX > 1.0f || scaleY > 1.0f) {
                poseStack.scale(scaleX, scaleY, 1.0f);
            }

            // 渲染静态遮罩底图 (在缩放矩阵内，会被拉伸覆盖整个区域)
            graphics.blit(RUMMAGE_MASK, 0, 0, 0, 0, 16, 16, 16, 16);

            // 弹出缩放矩阵，回到原始坐标系
            poseStack.popPose();

            // --- 阶段 2: 如果正在翻找，叠加渲染居中的动画图标 (不拉伸) ---
            if (slot.index == ClientRummageManager.currentRummageSlot) {
                int frame = (int) ((System.currentTimeMillis() / 100L) % 12);
                int vOffset = frame * 16;

                // 计算大物品区域的中心点 (使用原始坐标系)
                // 原点 (slot.x, slot.y) + (总像素宽高 / 2) - (图标 16像素 / 2)
                int centerX = slot.x + (pixelW / 2) - 8;
                int centerY = slot.y + (pixelH / 2) - 8;

                // 在原始坐标系渲染 16x16 的动画帧，保证画质清晰
                graphics.blit(IS_RUMMAGING, centerX, centerY, 0, vOffset, 16, 16, 16, 192);
            }

            ci.cancel();
        }
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void rummage$renderTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        Slot actualHovered = rummage$getActualSlot(this.hoveredSlot);
        if (actualHovered != null && ClientRummageManager.shouldMask(actualHovered.index)) {
            ci.cancel();
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rummage$slotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        Slot actualSlot = rummage$getActualSlot(slot);
        if (actualSlot != null && ClientRummageManager.shouldMask(actualSlot.index)) {
            ci.cancel();
        }
    }
}