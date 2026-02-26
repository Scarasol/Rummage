package com.scarasol.rummage.mixin;

import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 专门拦截原版漏斗专属的 ItemHandler。
 * 因为它继承自 InvWrapper 且单独重写了 insertItem 加入了冷却逻辑，所以必须单独拦截。
 * * @author Scarasol
 */
@Mixin(targets = "net.minecraftforge.items.VanillaHopperItemHandler", remap = false)
public abstract class VanillaHopperItemHandlerMixin extends InvWrapper {

    public VanillaHopperItemHandlerMixin(Container inv) {
        super(inv);
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void rummage$preventHopperInsert(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (CommonContainerUtil.isContainerLocked(this.getInv())) {
            cir.setReturnValue(stack);
        }
    }
}