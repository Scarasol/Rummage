package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 */
@Mixin(Containers.class)
public class ContainersMixin {

    @Inject(
            method = "dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/Container;)V",
            at = @At("HEAD")
    )
    private static void rummage$onEntityContainerDrop(Level level, Entity entity, Container container, CallbackInfo ci) {
        if (!level.isClientSide() && entity instanceof IRummageableEntity rummageable) {
            Player player = rummageable.getLastAttacker();
            if (player != null) {
                if (entity instanceof ContainerEntity containerEntity) {
                    containerEntity.unpackChestVehicleLootTable(player);
                }
                CommonContainerUtil.applyItemDestroy(rummageable, player, level.getRandom());
            }
        }
    }
}