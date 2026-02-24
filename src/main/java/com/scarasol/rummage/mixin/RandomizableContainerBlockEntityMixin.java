package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableContainerEntity;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.util.*;

/**
 * @author Scarasol
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin extends BaseContainerBlockEntity implements IRummageableContainerEntity {

    @Unique
    private final Set<UUID> rummage$fullyRummagedPlayer = new HashSet<>();

    @Unique
    private final Map<UUID, BitSet> rummage$rummageProgress = new HashMap<>();

    @Unique
    private final Set<UUID> rummage$rummagingPlayer = new HashSet<>();

    @Unique
    private boolean rummage$needRummage;

    public RandomizableContainerBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    private void rummage$setLootTable(ResourceLocation lootTable, long seed, CallbackInfo ci) {
        rummage$needRummage = true;
    }

    @Override
    public void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (rummage$needRummage) {
            CommonContainerUtil.savePlayList(compoundTag, rummage$fullyRummagedPlayer);
        }
        compoundTag.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, rummage$needRummage);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        if (compoundTag.contains(CommonContainerUtil.NEED_RUMMAGE_KEY)) {
            rummage$needRummage = compoundTag.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            if (compoundTag.contains(CommonContainerUtil.PLAYER_LIST_KEY)) {
                rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compoundTag));
            }
        } else if (compoundTag.contains("LootTable")){
            rummage$needRummage = true;
        }
    }

    @Unique
    @Override
    public Set<UUID> getFullyRummagedPlayer() {
        return rummage$fullyRummagedPlayer;
    }

    @Unique
    @Override
    public boolean isNeedRummage() {
        return rummage$needRummage;
    }

    @Unique
    @Override
    public Set<UUID> getRummagingPlayer() {
        return rummage$rummagingPlayer;
    }

    @Unique
    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return rummage$rummageProgress;
    }
}
