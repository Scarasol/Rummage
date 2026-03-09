package com.scarasol.rummage.mixin;

import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.Level;
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
 * 兼容原版运输船 (ChestBoat)
 * @author Scarasol
 */
@Mixin(ChestBoat.class)
public abstract class ChestBoatMixin extends Boat implements IRummageableEntity {

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

    // --- 追踪最后攻击者 ---
    @Unique
    private Player rummage$lastAttacker;

    public ChestBoatMixin(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    // 运输船的 setLootTable 只有一个带 ResourceLocation 参数的方法
    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("TAIL"))
    private void rummage$setLootTable(ResourceLocation lootTable, CallbackInfo ci) {
        if (!isInBlackList()) {
            this.rummage$needRummage = true;
        }
    }

    @Inject(method = "setChanged", at = @At("TAIL"))
    private void rummage$setChanged(CallbackInfo ci) {
        ChestBoat boat = (ChestBoat) (Object) this;
        // 如果战利品表已被解包（为null）且容器被完全清空，则不再需要翻找
        if (this.rummage$needRummage && this.lootTable == null && boat.isEmpty()) {
            this.rummage$needRummage = false;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rummage$addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, this.rummage$needRummage);
        CommonContainerUtil.savePlayList(compoundTag, this.rummage$fullyRummagedPlayer);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rummage$readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
        if (compoundTag.contains(CommonContainerUtil.NEED_RUMMAGE_KEY)) {
            this.rummage$needRummage = compoundTag.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            this.rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compoundTag));
        } else if (compoundTag.contains("LootTable")) {
            if (!isInBlackList()) {
                this.rummage$needRummage = true;
            }
        }
    }


    // --- 接口实现部分 ---

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
        return this.rummage$fullyRummagedPlayer;
    }

    @Override
    public boolean isNeedRummage() {
        return this.rummage$needRummage;
    }

    @Override
    public Set<UUID> getRummagingPlayer() {
        return this.rummage$rummagingPlayer;
    }

    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return this.rummage$rummageProgress;
    }

    // --- 调用实体原生的 getUUID() ---
    @Override
    public UUID getRummageableUUID() {
        return ((ChestBoat) (Object) this).getUUID();
    }


}