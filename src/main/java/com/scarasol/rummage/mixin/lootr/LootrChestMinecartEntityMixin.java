package com.scarasol.rummage.mixin.lootr;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.BitSet;
import java.util.UUID;

/**
 * 针对 Lootr 战利品矿车的兼容补丁
 * @author Scarasol
 */
@Mixin(LootrChestMinecartEntity.class)
public abstract class LootrChestMinecartEntityMixin implements IRummageableEntity {

    @Override
    public boolean isNeedRummage() {
        return true;
    }
}