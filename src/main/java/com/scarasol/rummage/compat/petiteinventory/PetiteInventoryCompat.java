package com.scarasol.rummage.compat.petiteinventory;

import com.sighs.petiteinventory.Config;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.utils.ClientUtils;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * 真实物品栏 (Petite Inventory) API 桥接类
 * 警告：外部调用本类任何方法前，必须确保 ModList.get().isLoaded("petiteinventory") 为 true！
 * @author Scarasol
 */
public class PetiteInventoryCompat {

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