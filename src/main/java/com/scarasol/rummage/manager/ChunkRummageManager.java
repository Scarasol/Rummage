package com.scarasol.rummage.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scarasol.rummage.data.ChunkRummageCache;
import com.scarasol.rummage.data.GlobalChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.scarasol.rummage.api.mixin.IRummageableEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 区块搜刮进度缓冲管理器。
 * 负责在区块卸载时暂时保存部分进度，避免数据丢失与 NBT 膨胀。
 *
 * @author Scarasol
 */
public class ChunkRummageManager {

    // 核心数据结构：Guava 并发缓存。自带 10 分钟 TTL 淘汰机制与极高并发性能。
    private static final Cache<GlobalChunkPos, ChunkRummageCache> GRACE_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES) // 写入后 10 分钟自动彻底销毁
                    .build();

    // ==================== 批量操作 (方块实体/区块加载卸载) ====================

    /**
     * 当区块卸载时调用（自动提取维度信息并构造 GlobalChunkPos）
     */
    public static void saveToGrace(@Nonnull Level level, @Nonnull ChunkPos chunkPos, Map<UUID, Map<UUID, BitSet>> data) {
        saveToGrace(new GlobalChunkPos(level.dimension(), chunkPos), data);
    }

    /**
     * 当区块加载时调用（自动提取维度信息并构造 GlobalChunkPos）
     */
    public static Map<UUID, Map<UUID, BitSet>> tryRestore(@Nonnull Level level, @Nonnull ChunkPos chunkPos) {
        return tryRestore(new GlobalChunkPos(level.dimension(), chunkPos));
    }

    /**
     * 核心方法：将数据封存进缓冲池。如果该区块已有缓存，则合并数据。
     */
    public static void saveToGrace(GlobalChunkPos pos, Map<UUID, Map<UUID, BitSet>> data) {
        if (data != null && !data.isEmpty()) {
            GRACE_CACHE.asMap().compute(pos, (key, existingCache) -> {
                if (existingCache == null) {
                    return new ChunkRummageCache(data); // 直接存！
                } else {
                    existingCache.progresses().putAll(data); // 直接合并！
                    return existingCache;
                }
            });
        }
    }

    /**
     * 核心方法：尝试极速取回并移除整个区块的缓存。
     */
    public static Map<UUID, Map<UUID, BitSet>> tryRestore(GlobalChunkPos pos) {
        ChunkRummageCache cache = GRACE_CACHE.getIfPresent(pos);
        if (cache != null) {
            GRACE_CACHE.invalidate(pos);
            return cache.progresses();
        }
        return null;
    }

    // ==================== 单体操作 (普通实体加载卸载) ====================

    /**
     * 实体卸载时调用：只存入单个实体的数据，合并到它所在的区块缓存中。
     */
    public static void saveSingleEntityGrace(@Nonnull Entity entity, @Nonnull IRummageableEntity rummageable) {
        if (!rummageable.isNeedRummage()) {
            return;
        }
        Map<UUID, BitSet> progress = rummageable.getRummageProgress();
        if (progress == null || progress.isEmpty()) {
            return;
        }
        GlobalChunkPos pos = new GlobalChunkPos(entity.level().dimension(), entity.chunkPosition());
        Map<UUID, Map<UUID, BitSet>> singleData = new HashMap<>();
        singleData.put(rummageable.getUUID(), progress);
        saveToGrace(pos, singleData);
    }

    /**
     * 单体精确提取：从区块缓存中精准提取并移除单个目标（方块实体/实体）的数据。
     * 不影响该区块内其他尚未加载的实体的缓存。
     * 适用于正常的 ChunkEvent.Load 和 EntityJoinLevelEvent。
     *
     * @param level      当前世界
     * @param chunkPos   所在区块坐标
     * @param targetUUID 目标的唯一 UUID
     * @return 目标的恢复数据，如果不存在则返回 null
     */
    @Nullable
    public static Map<UUID, BitSet> tryRestoreSingleEntity(@Nonnull Level level, @Nonnull ChunkPos chunkPos, @Nonnull UUID targetUUID) {
        GlobalChunkPos pos = new GlobalChunkPos(level.dimension(), chunkPos);
        ChunkRummageCache cache = GRACE_CACHE.getIfPresent(pos);

        if (cache != null) {
            // 仅把这个目标的进度拿出来，并从 Map 里删掉，O(1) 复杂度
            Map<UUID, BitSet> restored = cache.progresses().remove(targetUUID);

            // 如果拿走之后，这个区块缓存里没别的数据了，顺手把区块节点清理掉，释放内存
            if (cache.progresses().isEmpty()) {
                GRACE_CACHE.invalidate(pos);
            }
            return restored;
        }
        return null;
    }

    public static void clearAll() {
        GRACE_CACHE.invalidateAll();
        GRACE_CACHE.cleanUp();
    }

}