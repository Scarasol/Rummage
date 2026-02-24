package com.scarasol.rummage.util;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * @author Scarasol
 */
public class CommonContainerUtil {

    public static final String PLAYER_LIST_KEY = "RummagePlayList";
    public static final String NEED_RUMMAGE_KEY = "RummagePlayList";


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
}
