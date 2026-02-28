package com.scarasol.rummage.mixin.accessor;

import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Scarasol
 */
@Mixin(value = SidedInvWrapper.class, remap = false)
public interface SidedInvWrapperAccessor {

    /**
     * 获取 SidedInvWrapper 内部受保护的 inv 字段
     */
    @Accessor("inv")
    WorldlyContainer rummage$getInv();
}