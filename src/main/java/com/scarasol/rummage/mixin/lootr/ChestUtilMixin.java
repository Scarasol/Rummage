package com.scarasol.rummage.mixin.lootr;

import com.scarasol.rummage.api.mixin.ILootrInventoryDelegate;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
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
@Mixin(value = ChestUtil.class, remap = false)
public abstract class ChestUtilMixin {

    // 1. 使用最稳的 @Inject，并且用 shift = At.Shift.AFTER，在 openMenu 刚刚执行完的一瞬间拦截！
    @Inject(
            method = "handleLootChest",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", shift = At.Shift.AFTER, remap = true)
    )
    // 2. 直接拿 handleLootChest 方法自带的参数，绝不翻车！
    private static void rummage$bindLootrChestDelegate(Block block, Level level, BlockPos pos, Player player, CallbackInfo ci) {
        // 3. UI 已经打开了，直接去玩家的 containerMenu 里搜查！
        if (player.containerMenu != null) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof IRummageableEntity rummageable) {
                // 遍历刚刚打开的菜单里的每一个格子
                for (Slot slot : player.containerMenu.slots) {
                    // 如果发现格子的底层容器就是我们要找的 Lootr 容器
                    if (slot.container instanceof ILootrInventoryDelegate delegate) {
                        // 强行绑定！
                        delegate.rummage$setBlockEntity(rummageable);
                        break; // 绑定成功，收工！
                    }
                }
            }
        }
    }

    @Inject(
            method = "handleLootInventory",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", shift = At.Shift.AFTER, remap = true)
    )
    private static void rummage$bindLootrInventoryDelegate(Block block, Level level, BlockPos pos, Player player, CallbackInfo ci) {
        if (player.containerMenu != null) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof IRummageableEntity rummageable) {
                for (Slot slot : player.containerMenu.slots) {
                    if (slot.container instanceof ILootrInventoryDelegate delegate) {
                        delegate.rummage$setBlockEntity(rummageable);
                        break;
                    }
                }
            }
        }
    }

    @Inject(
            method = "handleLootCart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", shift = At.Shift.AFTER, remap = true)
    )
    private static void rummage$bindLootrCartDelegate(Level level, LootrChestMinecartEntity cart, Player player, CallbackInfo ci) {
        if (player.containerMenu != null) {
            if (cart instanceof IRummageableEntity rummageable) {
                for (Slot slot : player.containerMenu.slots) {
                    if (slot.container instanceof ILootrInventoryDelegate delegate) {
                        delegate.rummage$setBlockEntity(rummageable);
                        break;
                    }
                }
            }
        }
    }
}