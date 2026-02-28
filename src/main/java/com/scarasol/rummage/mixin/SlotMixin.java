package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.util.CommonContainerUtil;
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


    @Shadow
    public int index;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void rummage$mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot currentSlot = (Slot) (Object) this;

        if (player.level().isClientSide()) {
            if (com.scarasol.rummage.manager.ClientRummageManager.shouldMask(this.index)) {
                cir.setReturnValue(false);
            }
            return;
        }
        RummageTarget target = CommonContainerUtil.getTarget(currentSlot, null);
        if (target != null && !target.entity().isSlotRummaged(player, target.localSlotIndex())) {
            cir.setReturnValue(false);
        }
    }
}