package com.scarasol.rummage.compat.petiteinventory;

import com.scarasol.rummage.api.mixin.IRummageable;
import com.sighs.petiteinventory.Config;
import com.sighs.petiteinventory.api.PetiteInventoryAPI;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.utils.ClientUtils;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * 真实物品栏 (Petite Inventory) API 桥接类
 * @author Scarasol
 */
public class PetiteInventoryCompat {

    public static void init() {
        PetiteInventoryAPI.registerItemPositionChangedListener(event -> {
            if (event.getContainer() instanceof IRummageable rummageable) {

                if (!rummageable.isNeedRummage()) {
                    rummageable.setNeedRummage(true);
                }

            }
        });
    }

    public static boolean isMenuEnabled(AbstractContainerMenu menu) {
        if (menu == null) {
            return false;
        }

        // 玩家自身的背包 UI
        if (Config.ENABLE_INVENTORY.get() && menu instanceof InventoryMenu) {
            return true;
        }

        // 其他容器 UI（如箱子），完全遵循 Petite Inventory 的类名匹配逻辑
        String menuType = menu.getClass().toString();
        List<?> whitelist = Config.WHITELIST.get();
        if (whitelist != null) {
            return whitelist.contains(menuType);
        }

        return false;
    }

    public static int getWidth(ItemStack stack) {
        return Area.of(stack).width();
    }

    public static int getHeight(ItemStack stack) {
        return Area.of(stack).height();
    }

    public static Slot getMappedSlot(Slot slot) {
        return ClientUtils.getMappedSlot(slot);
    }
}