package com.scarasol.rummage.mixin.corpse;

import com.scarasol.rummage.api.mixin.ICorpseInventoryDelegate;
import com.scarasol.rummage.api.mixin.IRummageable;
import de.maxhenkel.corpse.corelib.inventory.ItemListInventory;
import de.maxhenkel.corpse.entities.CorpseEntity;
import de.maxhenkel.corpse.gui.CorpseContainerBase;
import de.maxhenkel.corpse.gui.CorpseInventoryContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 */
@Mixin(value = CorpseInventoryContainer.class, remap = false)
public abstract class CorpseInventoryContainerMixin extends CorpseContainerBase {

    @Shadow private ItemListInventory mainInventory;
    @Shadow private ItemListInventory armorInventory;
    @Shadow private ItemListInventory offHandInventory;

    public CorpseInventoryContainerMixin(MenuType type, int id, Inventory playerInventory, CorpseEntity corpse, boolean editable, boolean history) {
        super(type, id, playerInventory, corpse, editable, history);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rummage$bindCorpseToInventories(int id, Inventory playerInventory, CorpseEntity corpse, boolean editable, boolean history, CallbackInfo ci) {
        if (corpse instanceof IRummageable rummageable) {
            // 分配 Type: 0=主背包, 1=护甲, 2=副手
            if (this.mainInventory instanceof ICorpseInventoryDelegate delegateMain) {
                delegateMain.rummage$setCorpse(rummageable, 0);
            }
            if (this.armorInventory instanceof ICorpseInventoryDelegate delegateArmor) {
                delegateArmor.rummage$setCorpse(rummageable, 1);
            }
            if (this.offHandInventory instanceof ICorpseInventoryDelegate delegateOffhand) {
                delegateOffhand.rummage$setCorpse(rummageable, 2);
            }
        }
    }

    @Inject(method = "transferItems", at = @At("HEAD"), cancellable = true)
    private void rummage$preventTransferBeforeRummaged(CallbackInfo ci) {
        CorpseEntity corpse = this.getCorpse();
        if (corpse instanceof IRummageable rummageable) {
            CorpseInventoryContainer container = (CorpseInventoryContainer) (Object) this;
            if (container.getPlayerInventory() instanceof net.minecraft.world.entity.player.Inventory playerInv) {
                if (playerInv.player instanceof ServerPlayer serverPlayer) {
                    if (!rummageable.isFullyRummaged(serverPlayer)) {
                        ci.cancel(); // 未搜刮完拦截一键转移
                    }
                }
            }
        }
    }
}