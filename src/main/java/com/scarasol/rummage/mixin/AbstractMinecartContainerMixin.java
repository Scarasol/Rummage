package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 兼容原版带有物品栏的实体（如运输矿车、漏斗矿车等）
 * @author Scarasol
 */
@Mixin(AbstractMinecartContainer.class)
public abstract class AbstractMinecartContainerMixin extends AbstractMinecart implements ContainerEntity, IRummageableEntity {

    @Shadow
    @Nullable
    private ResourceLocation lootTable;

    @Unique
    private final Set<UUID> rummage$fullyRummagedPlayer = new HashSet<>();

    @Unique
    private final Map<UUID, BitSet> rummage$rummageProgress = new HashMap<>();

    @Unique
    private final Set<UUID> rummage$rummagingPlayer = new HashSet<>();

    @Unique
    private boolean rummage$needRummage;

    // --- 新增：用于防破坏的最后攻击者 ---
    @Unique
    private Player rummage$lastAttacker;

    protected AbstractMinecartContainerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    private void rummage$setLootTableWithSeed(ResourceLocation lootTable, long seed, CallbackInfo ci) {
        if (!isInBlackList()) {
            this.rummage$needRummage = true;
        }
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("TAIL"))
    private void rummage$setLootTableWithoutSeed(ResourceLocation lootTable, CallbackInfo ci) {
        if (!isInBlackList()) {
            this.rummage$needRummage = true;
        }
    }



    @Inject(method = "setChanged", at = @At("TAIL"))
    private void rummage$setChanged(CallbackInfo ci) {
        AbstractMinecartContainer container = (AbstractMinecartContainer) (Object) this;
        if (this.rummage$needRummage && this.lootTable == null && container.isEmpty()) {
            this.rummage$needRummage = false;

        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rummage$addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, rummage$needRummage);
        CommonContainerUtil.savePlayList(compoundTag, rummage$fullyRummagedPlayer);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rummage$readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        if (compoundTag.contains(CommonContainerUtil.NEED_RUMMAGE_KEY)) {
            rummage$needRummage = compoundTag.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compoundTag));
        } else if (compoundTag.contains("LootTable")) {
            if (!isInBlackList()) {
                rummage$needRummage = true;
            }
        }
    }

    @Override
    public boolean isInBlackList(){
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
        return CommonConfig.isBlacklisted(entityId);
    }

    @Override
    public void setNeedRummage(boolean needRummage) {
        this.rummage$needRummage = needRummage;
    }

    @Override
    public UUID getRummageableUUID() {
        return this.getUUID();
    }

    @Override
    public void setLastAttacker(@Nullable Player player) {
        this.rummage$lastAttacker = player;
    }

    @Nullable
    @Override
    public Player getLastAttacker() {
        return this.rummage$lastAttacker;
    }

    @Override
    public Set<UUID> getFullyRummagedPlayer() {
        return rummage$fullyRummagedPlayer;
    }

    @Override
    public boolean isNeedRummage() {
        return rummage$needRummage;
    }

    @Override
    public Set<UUID> getRummagingPlayer() {
        return rummage$rummagingPlayer;
    }

    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return rummage$rummageProgress;
    }

    // --- 注意：删除了 getUUID()，直接让实体原生的 getUUID() 接管！ ---


}