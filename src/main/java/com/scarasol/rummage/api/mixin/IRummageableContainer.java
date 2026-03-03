package com.scarasol.rummage.api.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.BitSet;
import java.util.UUID;

public interface IRummageableContainer extends Container, IRummageable {

    default boolean isFullyRummaged(Player player) {
        UUID playerUUID = player.getUUID();
        if (!this.isNeedRummage(playerUUID)) {
            return true;
        }
        BitSet progress = this.getRummageProgressByUUID(playerUUID);
        int containerSize = this.getContainerSize();
        return progress.cardinality() >= containerSize;
    }

    default void addFullyRummagedPlayer(UUID playerUUID) {
        getFullyRummagedPlayer().add(playerUUID);
        this.setChanged();
    }
}
