package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Scarasol
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    @Final
    public Container container;


    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void rummage$mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof IRummageableEntity rummageable) {
            Slot currentSlot = (Slot) (Object) this;
            if (!rummageable.isSlotRummaged(player, currentSlot.getContainerSlot())) {
                cir.setReturnValue(false);
            }
        }
    }
}