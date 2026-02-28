package com.scarasol.rummage.util;

import com.scarasol.rummage.RummageMod;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.mixin.accessor.CompoundContainerAccessor;
import com.scarasol.rummage.mixin.accessor.SidedInvWrapperAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Scarasol
 */
public class CommonContainerUtil {

    public static final String PLAYER_LIST_KEY = "FullyRummagePlayList";
    public static final String NEED_RUMMAGE_KEY = "IsNeedRummage";
    public static final String RUMMAGE_UUID_KEY = "RummageUUID";

    private static final Map<Class<?>, Field> MCR_FIELD_CACHE = new HashMap<>();
    private static final Set<Class<?>> NOT_MCR_MENUS = new HashSet<>();

    public static final ThreadLocal<Boolean> UI_ACTION_BYPASS = ThreadLocal.withInitial(() -> false);


    public static boolean containsPlayer(Player player, IRummageableEntity rummageableEntity) {
        return rummageableEntity.getFullyRummagedPlayer().contains(player.getUUID());
    }

    public static boolean isNeedRummage(Player player, IRummageableEntity rummageableEntity) {
        return !containsPlayer(player, rummageableEntity);
    }

    public static void savePlayList(CompoundTag tag, Collection<UUID> playList) {
        ListTag listTag = new ListTag();
        for (UUID uuid : playList) {
            listTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put(PLAYER_LIST_KEY, listTag);
    }

    public static Set<UUID> loadPlayList(CompoundTag tag) {
        Set<UUID> playList = new HashSet<>();
        if (tag.contains(PLAYER_LIST_KEY, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(PLAYER_LIST_KEY, Tag.TAG_INT_ARRAY);
            for (Tag value : listTag) {
                playList.add(NbtUtils.loadUUID(value));
            }
        }
        return playList;
    }

    public static boolean isContainerLocked(Container container) {
        if (container == null) {
            return false;
        }

        if (container instanceof IRummageableEntity rummageable && rummageable.isNeedRummage()) {
            return true;
        }

        if (container instanceof CompoundContainer compound) {
            Container c1 = ((CompoundContainerAccessor) compound).rummage$getContainer1();
            Container c2 = ((CompoundContainerAccessor) compound).rummage$getContainer2();
            return isContainerLocked(c1) || isContainerLocked(c2);
        }

        return false;
    }

    /**
     * 哎我草 MCR 怎么这么喜欢内部类啊
     *
     * @param slot 当前判断的格子
     * @param menu 玩家当前打开的菜单 (用于 MCR 兼容 fallback)
     * @return RummageTarget 翻找目标，如果不需要翻找则返回 null
     */
    public static RummageTarget getTarget(Slot slot, AbstractContainerMenu menu) {
        if (slot == null) {
            return null;
        }

        // 1. 常规判断：解析 slot.container
        RummageTarget target = getTarget(slot.container, slot.getContainerSlot());
        if (target != null) {
            return target;
        }

        // 2. 处理 Forge 相关的特殊 Slot
        if (slot instanceof SlotItemHandler slotItemHandler) {
            int slotIndex = slot.getContainerSlot();

            // 尝试 A：规范的 Forge 包装器
            target = getWrappedTarget(slotItemHandler.getItemHandler(), slotIndex);
            if (target != null) {
                return target;
            }

            // 尝试 B：游离的 ItemStackHandler
            return getMcrTarget(menu, slotIndex);
        }

        return null;
    }

    /**
     * 解析规范的 Forge 模组容器包装器
     */
    private static RummageTarget getWrappedTarget(IItemHandler handler, int slotIndex) {
        Container wrappedContainer = null;

        if (handler instanceof InvWrapper invWrapper) {
            wrappedContainer = invWrapper.getInv();
        } else if (handler instanceof SidedInvWrapper sidedInvWrapper) {

            wrappedContainer = ((SidedInvWrapperAccessor) sidedInvWrapper).rummage$getInv();
        }

        if (wrappedContainer instanceof IRummageableEntity rummageable) {
            return new RummageTarget(rummageable, slotIndex);
        }

        return null;
    }

    /**
     * 解析 MCreator 类模组的游离容器（带反射与缓存机制）
     */
    private static RummageTarget getMcrTarget(AbstractContainerMenu menu, int slotIndex) {
        if (menu == null) {
            return null;
        }

        Class<?> menuClass = menu.getClass();

        if (NOT_MCR_MENUS.contains(menuClass)) {

            return null;
        }

        try {
            Field field = MCR_FIELD_CACHE.get(menuClass);
            if (field == null) {
                field = menuClass.getDeclaredField("boundBlockEntity");
                field.setAccessible(true);
                MCR_FIELD_CACHE.put(menuClass, field);
            }

            Object obj = field.get(menu);
            if (obj instanceof IRummageableEntity rummageable) {
                return new RummageTarget(rummageable, slotIndex);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 访问异常或找不到字段，加入黑名单
            NOT_MCR_MENUS.add(menuClass);
        }

        return null;
    }

    public static RummageTarget getTarget(Container container, int slotIndex) {
        if (container instanceof CompoundContainer compound) {
            Container c1 = ((CompoundContainerAccessor) compound).rummage$getContainer1();
            Container c2 = ((CompoundContainerAccessor) compound).rummage$getContainer2();
            int size1 = c1.getContainerSize();

            if (slotIndex >= size1) {
                return getTarget(c2, slotIndex - size1);
            } else {
                return getTarget(c1, slotIndex);
            }
        }

        if (container instanceof IRummageableEntity rummageable) {
            return new RummageTarget(rummageable, slotIndex);
        }

        return null;
    }
}
