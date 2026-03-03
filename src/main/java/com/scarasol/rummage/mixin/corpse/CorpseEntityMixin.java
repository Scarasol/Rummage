package com.scarasol.rummage.mixin.corpse;

import com.scarasol.rummage.api.mixin.ICorpseRummageable;
import com.scarasol.rummage.api.mixin.IRummageable;
import com.scarasol.rummage.configuration.CommonConfig;
import com.scarasol.rummage.util.CommonContainerUtil;
import de.maxhenkel.corpse.corelib.death.Death;
import de.maxhenkel.corpse.entities.CorpseBoundingBoxBase;
import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * @author Scarasol
 */
@Mixin(CorpseEntity.class)
public abstract class CorpseEntityMixin extends CorpseBoundingBoxBase implements IRummageable, ICorpseRummageable {

    public CorpseEntityMixin(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Shadow public abstract Optional<UUID> getCorpseUUID();
    @Shadow public abstract Death getDeath();

    @Unique
    private final Set<UUID> rummage$fullyRummagedPlayer = new HashSet<>();
    @Unique
    private final Set<UUID> rummage$rummagingPlayer = new HashSet<>();
    @Unique
    private boolean rummage$needRummage;


    @Unique private final Map<UUID, BitSet> rummage$progressMain = new HashMap<>();
    @Unique private final Map<UUID, BitSet> rummage$progressArmor = new HashMap<>();
    @Unique private final Map<UUID, BitSet> rummage$progressOffhand = new HashMap<>();
    @Unique private final Map<UUID, BitSet> rummage$progressAdditional = new HashMap<>();

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void rummage$initBlacklistState(EntityType<?> entityTypeIn, Level worldIn, CallbackInfo ci) {

        this.rummage$needRummage = !this.isInBlackList();
    }

    @Override
    public Map<UUID, BitSet> rummage$getProgress(int type) {
        if (type == 1) return rummage$progressArmor;
        if (type == 2) return rummage$progressOffhand;
        if (type == 3) return rummage$progressAdditional;
        return rummage$progressMain;
    }

    // 提供给外部调用的默认项
    @Override
    public Map<UUID, BitSet> getRummageProgress() {
        return rummage$progressMain;
    }

    @Override
    public void setNeedRummage(boolean needRummage) {
        this.rummage$needRummage = needRummage;
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
    public UUID getRummageableUUID() {
        return ((CorpseEntity) (Object) this).getUUID();
    }

    @Override
    public boolean isNeedRummage(UUID playerUUID) {
        Optional<UUID> corpseOwner = this.getCorpseUUID();
        if (corpseOwner.isPresent() && corpseOwner.get().equals(playerUUID)) {
            return false; // 主人免检
        }

        return this.isNeedRummage() && !this.getFullyRummagedPlayer().contains(playerUUID);
    }

    @Override
    public boolean isInBlackList(){
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
        return CommonConfig.isBlacklisted(entityId);
    }

    /**
     * 清理所有子容器的缓存
     */
    @Override
    public void removeRummageProgressByUUID(UUID playerUUID) {
        rummage$progressMain.remove(playerUUID);
        rummage$progressArmor.remove(playerUUID);
        rummage$progressOffhand.remove(playerUUID);
        rummage$progressAdditional.remove(playerUUID);
    }

    /**
     * 统筹判定：所有子容器里的物品都被搜刮完才算完成
     */
    @Override
    public boolean isFullyRummaged(Player player) {
        UUID uuid = player.getUUID();
        if (!this.isNeedRummage(uuid)) return true;

        Death death = this.getDeath();
        if (death == null) return true;

        if (!checkFullyRummaged(rummage$progressMain.get(uuid), death.getMainInventory())) return false;
        if (!checkFullyRummaged(rummage$progressArmor.get(uuid), death.getArmorInventory())) return false;
        if (!checkFullyRummaged(rummage$progressOffhand.get(uuid), death.getOffHandInventory())) return false;
        if (!checkFullyRummaged(rummage$progressAdditional.get(uuid), death.getAdditionalItems())) return false;

        return true;
    }

    @Unique
    private boolean checkFullyRummaged(BitSet progress, List<ItemStack> items) {
        if (progress == null) {
            // 没有进度记录，说明一格都没点过，只要有物品就返回未完成
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) return false;
            }
            return true;
        }
        int index = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && !progress.get(index)) return false;
            index++;
        }
        return true;
    }

    @Override
    public BitSet initRummageBitSet() {
        return new BitSet(); // 初始化由各个子临时容器代劳
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rummage$addAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        compound.putBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY, rummage$needRummage);
        CommonContainerUtil.savePlayList(compound, rummage$fullyRummagedPlayer);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rummage$readAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains(CommonContainerUtil.NEED_RUMMAGE_KEY)) {
            rummage$needRummage = compound.getBoolean(CommonContainerUtil.NEED_RUMMAGE_KEY);
            rummage$fullyRummagedPlayer.addAll(CommonContainerUtil.loadPlayList(compound));
        } else {
            rummage$needRummage = true;
        }
    }
}