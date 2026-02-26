package com.scarasol.rummage.network;

import com.scarasol.rummage.manager.ClientRummageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.BitSet;
import java.util.function.Supplier;

public record SyncRummageStatePacket(BitSet maskedMenuSlots) {

    public static SyncRummageStatePacket decode(FriendlyByteBuf buf) {
        return new SyncRummageStatePacket(BitSet.valueOf(buf.readByteArray()));
    }

    public static void encode(SyncRummageStatePacket msg, FriendlyByteBuf buf) {
        buf.writeByteArray(msg.maskedMenuSlots().toByteArray());
    }

    public static void handler(SyncRummageStatePacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (msg != null && context.get().getDirection().getReceptionSide().isClient()) {
                ClientPacketHandler.handle(msg);
            }
        });
        context.get().setPacketHandled(true);
    }
}