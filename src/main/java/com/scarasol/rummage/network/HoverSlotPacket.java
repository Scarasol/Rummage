package com.scarasol.rummage.network;

import com.scarasol.rummage.api.mixin.IRummageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record HoverSlotPacket(int slotIndex) {

    public static HoverSlotPacket decode(FriendlyByteBuf buf) {
        return new HoverSlotPacket(buf.readInt());
    }

    public static void encode(HoverSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotIndex());
    }

    public static void handler(HoverSlotPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu != null) {
                ((IRummageMenu) player.containerMenu).rummage$setHoveredSlot(msg.slotIndex());
            }
        });
        context.get().setPacketHandled(true);
    }
}