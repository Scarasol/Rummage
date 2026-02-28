package com.scarasol.rummage.mixin.lootr;

import com.llamalad7.mixinextras.sugar.Local;
import com.scarasol.rummage.api.mixin.ILootrInventoryDelegate;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import noobanidus.mods.lootr.util.ChestUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 */
@Mixin(ChestUtil.class)
public abstract class ChestUtilMixin {

    @Inject(
            method = "handleLootChest",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;")
    )
    private static void rummage$bindLootrChestDelegate(Block block, Level level, BlockPos pos, Player player, CallbackInfo ci, @Local MenuProvider provider) {
        if (provider instanceof ILootrInventoryDelegate delegate) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof IRummageableEntity rummageable) {
                delegate.rummage$setBlockEntity(rummageable);
            }
        }
    }

    @Inject(
            method = "handleLootInventory",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;")
    )
    private static void rummage$bindLootrInventoryDelegate(Block block, Level level, BlockPos pos, Player player, CallbackInfo ci, @Local MenuProvider provider) {
        if (provider instanceof ILootrInventoryDelegate delegate) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof IRummageableEntity rummageable) {
                delegate.rummage$setBlockEntity(rummageable);
            }
        }
    }

    @Inject(
            method = "handleLootCart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;")
    )
    private static void rummage$bindLootrCartDelegate(Level level, LootrChestMinecartEntity cart, Player player, CallbackInfo ci, @Local MenuProvider provider) {
        if (provider instanceof ILootrInventoryDelegate delegate) {
            if (cart instanceof IRummageableEntity rummageable) {
                delegate.rummage$setBlockEntity(rummageable);
            }
        }
    }
}