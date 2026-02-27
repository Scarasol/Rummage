package com.scarasol.rummage.event;

import com.scarasol.rummage.api.mixin.IRummageMenu;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.data.RummageTarget;
import com.scarasol.rummage.manager.ChunkRummageManager;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.SyncRummageStatePacket;
import com.scarasol.rummage.util.CommonContainerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof IRummageMenu rummageMenu) {
            rummageMenu.rummage$setActivePlayer(player);
        }

        BitSet maskedMenuSlots = new BitSet();
        Set<IRummageableEntity> uniqueContainers = new HashSet<>();

        // 穿透提取真实容器
        for (Slot slot : menu.slots) {
            RummageTarget target = CommonContainerUtil.getTarget(slot.container, slot.getContainerSlot());
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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

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
}
