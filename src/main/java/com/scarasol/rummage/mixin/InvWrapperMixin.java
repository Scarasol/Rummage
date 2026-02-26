package com.scarasol.rummage.mixin;

import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Scarasol
 */
@Mixin(targets = "net.minecraftforge.items.wrapper.InvWrapper", remap = false)
public abstract class InvWrapperMixin {

    @Shadow public abstract Container getInv();

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void rummage$preventInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CommonContainerUtil.isContainerLocked(this.getInv())) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void rummage$preventExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CommonContainerUtil.isContainerLocked(this.getInv())) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}