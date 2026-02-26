package com.scarasol.rummage.api.mixin;

import net.minecraft.world.entity.player.Player;

/**
 * @author Scarasol
 */
public interface IRummageMenu {
    void rummage$setHoveredSlot(int slotIndex);

    void rummage$setActivePlayer(Player player);
}