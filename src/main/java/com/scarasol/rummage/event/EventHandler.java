package com.scarasol.rummage.event;

import com.scarasol.rummage.api.mixin.IRummageMenu;
import com.scarasol.rummage.api.mixin.IRummageableEntity;
import com.scarasol.rummage.manager.ChunkRummageManager;
import com.scarasol.rummage.network.NetworkHandler;
import com.scarasol.rummage.network.SyncRummageStatePacket;
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) {
            return;
        }

        AbstractContainerMenu menu = event.getContainer();
        ((IRummageMenu) menu).rummage$setActivePlayer(player);
        BitSet maskedMenuSlots = new BitSet();

        // 遍历整个 UI 的格子（服务端这里的 container 是真实的实体！）
        for (Slot slot : menu.slots) {
            if (slot.container instanceof IRummageableEntity rummageable) {
                // 如果需要搜刮，且当前格子还没有被该玩家搜刮完毕
                if (rummageable.isNeedRummage(player.getUUID()) &&
                        !rummageable.isSlotRummaged(player, slot.getContainerSlot())) {

                    // 将该格子在 Menu 中的绝对索引设为 true (需要遮罩)
                    maskedMenuSlots.set(slot.index);
                }
            }
        }

        // 发送给客户端
        if (!maskedMenuSlots.isEmpty()) {
            NetworkHandler.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncRummageStatePacket(maskedMenuSlots)
            );
        }
    }
}
