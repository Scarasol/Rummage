package com.scarasol.rummage.mixin;

import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截所有带面/方向的容器（如木桶、潜影盒、熔炉等 WorldlyContainer）的物流传输。
 * * @author Scarasol
 */
@Mixin(targets = "net.minecraftforge.items.wrapper.SidedInvWrapper", remap = false)
public abstract class SidedInvWrapperMixin {

    @Shadow protected WorldlyContainer inv;

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void rummage$preventInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CommonContainerUtil.isContainerLocked(this.inv)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void rummage$preventExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CommonContainerUtil.isContainerLocked(this.inv)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}