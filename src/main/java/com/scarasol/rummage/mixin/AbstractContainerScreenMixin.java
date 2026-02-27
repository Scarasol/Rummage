package com.scarasol.rummage.mixin;

import com.scarasol.rummage.manager.ClientRummageManager;
import com.scarasol.rummage.network.HoverSlotPacket;
import com.scarasol.rummage.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
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

    @Unique
    private static final ResourceLocation RUMMAGE_MASK = new ResourceLocation(MODID, "textures/gui/need_rummage/need_rummage.png");

    @Unique
    private static final ResourceLocation IS_RUMMAGING = new ResourceLocation(MODID, "textures/gui/need_rummage/is_rummaging.png");

    @Unique
    private int rummage$pendingHoverIndex = -1;
    @Unique
    private int rummage$lastSentHoverIndex = -1;
    @Unique
    private long rummage$hoverStartTime = 0L;
    @Unique
    private static final long HOVER_DELAY_MS = 500L;

    /**
     * 检测鼠标悬停变化，加入防抖判定后同步给服务端
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void rummage$detectHoverChange(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int currentIndex = (this.hoveredSlot != null) ? this.hoveredSlot.index : -1;

        // 1. 如果鼠标物理悬停的格子发生了变化，重置计时器
        if (currentIndex != rummage$pendingHoverIndex) {
            rummage$pendingHoverIndex = currentIndex;
            rummage$hoverStartTime = System.currentTimeMillis();
        }

        // 2. 检查悬停时间是否已经达到了 0.5 秒
        if (System.currentTimeMillis() - rummage$hoverStartTime >= HOVER_DELAY_MS) {
            // 3. 如果确认注视的目标和上次发包的不同，进行发包更新
            if (rummage$pendingHoverIndex != rummage$lastSentHoverIndex) {
                rummage$lastSentHoverIndex = rummage$pendingHoverIndex;

                // 只有悬停在需要遮罩的格子上，或者是移出所有格子时（-1，让服务端切回自动翻找），才发包
                if (rummage$pendingHoverIndex == -1 || ClientRummageManager.shouldMask(rummage$pendingHoverIndex)) {
                    NetworkHandler.PACKET_HANDLER.sendToServer(new HoverSlotPacket(rummage$pendingHoverIndex));
                }
            }
        }
    }

    /**
     * 遮罩与动画贴图渲染
     */
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void rummage$renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (ClientRummageManager.shouldMask(slot.index)) {

            if (slot.index == ClientRummageManager.currentRummageSlot) {
                int frame = (int) ((System.currentTimeMillis() / 100L) % 12);
                int vOffset = frame * 16;
                graphics.blit(IS_RUMMAGING, slot.x, slot.y, 0, vOffset, 16, 16, 16, 192);
            } else {
                // 如果不是，渲染基础的“未搜刮”遮罩
                graphics.blit(RUMMAGE_MASK, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
            }

            ci.cancel();
        }
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void rummage$renderTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (this.hoveredSlot != null && ClientRummageManager.shouldMask(this.hoveredSlot.index)) {
            ci.cancel();
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rummage$slotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (slot != null && ClientRummageManager.shouldMask(slot.index)) {
            ci.cancel();
        }
    }
}