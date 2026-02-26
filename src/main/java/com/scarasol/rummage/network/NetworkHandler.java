package com.scarasol.rummage.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.scarasol.rummage.RummageMod.MODID;

/**
 * @author Scarasol
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, MODID),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int messageID = 0;

    public static void addNetworkMessage() {
        // 1. 初始同步包 (S2C)：玩家打开容器时，服务端下发所有需要遮罩的格子索引
        PACKET_HANDLER.registerMessage(messageID++,
                SyncRummageStatePacket.class,
                SyncRummageStatePacket::encode,
                SyncRummageStatePacket::decode,
                SyncRummageStatePacket::handler);

        // 2. 悬停状态包 (C2S)：客户端鼠标悬停格子发生变化时，通知服务端优先翻找
        PACKET_HANDLER.registerMessage(messageID++,
                HoverSlotPacket.class,
                HoverSlotPacket::encode,
                HoverSlotPacket::decode,
                HoverSlotPacket::handler);

        // 3. 动作指令包 (S2C)：服务端算好进度后，命令客户端播放进度条动画或解除遮罩
        PACKET_HANDLER.registerMessage(messageID++,
                RummageActionPacket.class,
                RummageActionPacket::encode,
                RummageActionPacket::decode,
                RummageActionPacket::handler);
    }
}