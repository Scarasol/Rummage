package com.scarasol.rummage.event;

import com.scarasol.rummage.api.mixin.IRummageMenu;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.manager.ChunkRummageManager;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.SyncRummageStatePacket;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

import static com.scarasol.rummage.configuration.CommonConfig.SPEC;
import static com.scarasol.rummage.configuration.CommonConfig.markDirty;

/**
 * @author Scarasol
 */
@Mod.EventBusSubscriber
public class EventHandler {

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk chunk) {
            Level level = chunk.getLevel();
            ChunkPos chunkPos = chunk.getPos();
            Map<UUID, Map<UUID, BitSet>> transferData = new HashMap<>();
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof IRummageableEntity r && r.isNeedRummage()) {
                    Map<UUID, BitSet> originalProgress = r.getRummageProgress();
                    if (originalProgress != null && !originalProgress.isEmpty()) {
                        Map<UUID, BitSet> clonedProgress = new HashMap<>();
                        for (Map.Entry<UUID, BitSet> entry : originalProgress.entrySet()) {
                            clonedProgress.put(entry.getKey(), (BitSet) entry.getValue().clone());
                        }
                        transferData.put(r.getUUID(), clonedProgress);
                    }
                }
            }

            if (!transferData.isEmpty()) {
                ChunkRummageManager.saveToGrace(level, chunkPos, transferData);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk) {
            Level level = chunk.getLevel();
            ChunkPos chunkPos = chunk.getPos();
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof IRummageableEntity r) {
                    Map<UUID, BitSet> saved = ChunkRummageManager.tryRestoreSingleEntity(level, chunkPos, r.getUUID());
                    if (saved != null) {
                        r.getRummageProgress().putAll(saved);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
            return;
        }
        if (entity instanceof IRummageableEntity r && r.isNeedRummage()) {
            Map<UUID, BitSet> originalProgress = r.getRummageProgress();

            if (originalProgress != null && !originalProgress.isEmpty()) {
                Map<UUID, BitSet> clonedProgress = new HashMap<>();
                for (Map.Entry<UUID, BitSet> entry : originalProgress.entrySet()) {
                    clonedProgress.put(entry.getKey(), (BitSet) entry.getValue().clone());
                }

                Map<UUID, Map<UUID, BitSet>> singleData = new HashMap<>();
                singleData.put(r.getUUID(), clonedProgress);
                ChunkRummageManager.saveToGrace(entity.level(), entity.chunkPosition(), singleData);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.loadedFromDisk() && entity instanceof IRummageableEntity r) {

            Map<UUID, BitSet> saved = ChunkRummageManager.tryRestoreSingleEntity(entity.level(), entity.chunkPosition(), r.getUUID());
            if (saved != null) {
                r.getRummageProgress().putAll(saved);
            }
        }
    }

    @SubscribeEvent
    public static void onWatch(ChunkWatchEvent.Watch event) {

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof IRummageMenu rummageMenu) {
            rummageMenu.rummage$setActivePlayer(player);
        }

        BitSet maskedMenuSlots = new BitSet();
        Set<IRummageableEntity> uniqueContainers = new HashSet<>();

        // 穿透提取真实容器
        for (Slot slot : menu.slots) {
            RummageTarget target = CommonContainerUtil.getTarget(slot, menu);
            if (target != null) {
                // 收集真实的容器实体（大箱子会自动收集到左半边和右半边两个独立的实体）
                uniqueContainers.add(target.entity());

                if (target.entity().isNeedRummage(player.getUUID())) {
                    if (!target.entity().isSlotRummaged(player, target.localSlotIndex())) {
                        maskedMenuSlots.set(slot.index);
                    }
                }
            }
        }

        // 统一注册玩家 UUID
        for (IRummageableEntity rummageable : uniqueContainers) {
            rummageable.getRummagingPlayer().add(player.getUUID());
        }

        if (!maskedMenuSlots.isEmpty()) {
            NetworkHandler.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncRummageStatePacket(maskedMenuSlots)
            );
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();
        Set<IRummageableEntity> uniqueContainers = new HashSet<>();

        for (Slot slot : menu.slots) {
            RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());
            if (target != null) {
                uniqueContainers.add(target.entity());
            }
        }

        // 统一注销玩家 UUID
        for (IRummageableEntity rummageable : uniqueContainers) {
            rummageable.getRummagingPlayer().remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            markDirty();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // 服务端彻底停止（无论是独立服务端关闭，还是单人模式退出存档）时触发
        ChunkRummageManager.clearAll();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockEntity be = levelAccessor.getBlockEntity(pos);

        if (be instanceof IRummageableEntity rummageable && rummageable.isNeedRummage(player.getUUID())) {

            // --- 关键防御：应对战利品表懒加载 ---
            // 如果它是一个原版的战利品箱子，且还没有解包（里面有 LootTable 标签）
            if (be instanceof RandomizableContainerBlockEntity randomizable) {
                // 强制让它利用当前玩家的身份去生成战利品到格子里
                randomizable.unpackLootTable(player);
            }

            // 物品损毁概率，建议写入 CommonConfig
            double destroyChance = 0.5;

            // 1. 拦截原版 Container (现在里面已经有生成好的战利品了)
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (!rummageable.isSlotRummaged(player, i) && !container.getItem(i).isEmpty()) {
                        if (levelAccessor.getRandom().nextFloat() < destroyChance) {
                            container.setItem(i, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }
}
