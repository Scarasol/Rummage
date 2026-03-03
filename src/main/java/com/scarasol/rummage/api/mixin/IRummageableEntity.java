package com.scarasol.rummage.api.mixin;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * @author Scarasol
 */
public interface IRummageableEntity extends IRummageableContainer{


    void setLastAttacker(@Nullable Player player);

    @Nullable
    Player getLastAttacker();

}
