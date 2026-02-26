package com.scarasol.rummage.mixin.accessor;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Scarasol
 * 专门用于获取大箱子 (CompoundContainer) 内部私有容器的访问器
 */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {

    @Accessor("container1")
    Container rummage$getContainer1();

    @Accessor("container2")
    Container rummage$getContainer2();
}