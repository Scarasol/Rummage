package com.scarasol.rummage.network;

import com.scarasol.rummage.manager.ClientRummageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record RummageActionPacket(int slotIndex, int action, int totalTicks) {
    // action: 0 = 停止当前动画, 1 = 开始播放动画, 2 = 彻底解锁(清除遮罩)

    public static RummageActionPacket decode(FriendlyByteBuf buf) {
        return new RummageActionPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void encode(RummageActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotIndex());
        buf.writeInt(msg.action());
        buf.writeInt(msg.totalTicks());
    }

    public static void handler(RummageActionPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection().getReceptionSide().isClient()) {
                ClientPacketHandler.handleAction(msg);
            }
        });
        context.get().setPacketHandled(true);
    }
}