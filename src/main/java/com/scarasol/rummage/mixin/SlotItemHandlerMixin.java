package com.scarasol.rummage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author Scarasol
 */
@Mixin(SlotItemHandler.class)
public abstract class SlotItemHandlerMixin {

    @WrapOperation(
            method = {
                    "mayPickup(Lnet/minecraft/world/entity/player/Player;)Z",
                    "remove(I)Lnet/minecraft/world/item/ItemStack;"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", remap = false)
    )
    private ItemStack rummage$bypassExtract(IItemHandler handler, int slot, int amount, boolean simulate, Operation<ItemStack> original) {
        CommonContainerUtil.UI_ACTION_BYPASS.set(true);
        try {
            return original.call(handler, slot, amount, simulate);
        } finally {
            CommonContainerUtil.UI_ACTION_BYPASS.remove();
        }
    }

    @WrapOperation(
            method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", remap = false)
    )
    private ItemStack rummage$bypassInsertNormal(IItemHandler handler, int slot, ItemStack stack, boolean simulate, Operation<ItemStack> original) {
        CommonContainerUtil.UI_ACTION_BYPASS.set(true);
        try {
            return original.call(handler, slot, stack, simulate);
        } finally {
            CommonContainerUtil.UI_ACTION_BYPASS.remove();
        }
    }

    @WrapOperation(
            method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandlerModifiable;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", remap = false)
    )
    private ItemStack rummage$bypassInsertModifiable(net.minecraftforge.items.IItemHandlerModifiable handler, int slot, ItemStack stack, boolean simulate, Operation<ItemStack> original) {
        CommonContainerUtil.UI_ACTION_BYPASS.set(true);
        try {
            return original.call(handler, slot, stack, simulate);
        } finally {
            CommonContainerUtil.UI_ACTION_BYPASS.remove();
        }
    }
}