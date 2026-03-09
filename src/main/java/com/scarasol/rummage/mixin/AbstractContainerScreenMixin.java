package com.scarasol.rummage.mixin;

import com.scarasol.rummage.compat.ModCompat;
import com.scarasol.rummage.compat.petiteinventory.PetiteInventoryCompat;
import com.scarasol.rummage.manager.ClientRummageManager;
import com.scarasol.rummage.util.ClientScreenUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
    private Slot rummage$getActualSlot(@Nullable Slot original) {
        if (original != null && ModCompat.isLoadPetiteInventory()) {
            if (PetiteInventoryCompat.isMenuEnabled(this.menu)) {
                return PetiteInventoryCompat.getMappedSlot(original);
            }
        }
        return original;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void rummage$detectHoverChange(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Slot actualHovered = rummage$getActualSlot(this.hoveredSlot);
        ClientScreenUtil.handleHoverAndSound(actualHovered);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void rummage$renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (ClientRummageManager.shouldMask(slot.index)) {
            ClientScreenUtil.renderRummageMask(graphics, slot, this.menu);
            ci.cancel();
        }
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void rummage$renderSlotFlash(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        // 当格子没有被遮罩时，尝试渲染连锁闪烁高亮
        if (!ClientRummageManager.shouldMask(slot.index)) {
            ClientScreenUtil.renderFlashHighlight(graphics, slot, this.menu);
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