package com.scarasol.rummage.api.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Scarasol
 */
public interface IRummageableEntity {

    Set<UUID> getFullyRummagedPlayer();

    boolean isNeedRummage();

    Set<UUID> getRummagingPlayer();

    boolean isFullyRummaged(Player player);

    Map<UUID, BitSet> getRummageProgress();

    default boolean isNeedRummage(UUID playerUUID) {
        return this.isNeedRummage() && !this.getFullyRummagedPlayer().contains(playerUUID);
    }

    default BitSet getRummageProgressByUUID(UUID playerUUID) {
        return this.getRummageProgress().computeIfAbsent(playerUUID, k -> new BitSet());
    }

    default void removeRummageProgressByUUID(UUID playerUUID) {
        this.getRummageProgress().remove(playerUUID);
    }

    default boolean isSlotRummaged(Player player, int slotIndex) {
        UUID playerUUID = player.getUUID();
        if (!this.isNeedRummage(playerUUID)) {
            return true;
        }
        BitSet progress = this.getRummageProgressByUUID(playerUUID);
        return progress.get(slotIndex);
    }

    default void markSlotRummaged(Player player, int slotIndex) {
        UUID playerUUID = player.getUUID();
        if (this.isFullyRummaged(player)) {
            return;
        }
        BitSet progress = this.getRummageProgressByUUID(playerUUID);
        progress.set(slotIndex);
        if (isFullyRummaged(player)) {
            this.getFullyRummagedPlayer().add(playerUUID);
            this.removeRummageProgressByUUID(playerUUID);
            this.getRummagingPlayer().remove(playerUUID);
        }
    }
}
