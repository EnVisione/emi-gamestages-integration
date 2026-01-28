package com.enviouse.emi_gamestages_link.common.network;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.common.LockSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Handles network packet registration and sending.
 * This class is safe to load on both client and server.
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EmiGameStagesLink.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    /**
     * Register all packets. Call this during mod construction.
     */
    public static void register() {
        // S2C: Full lock data sync
        CHANNEL.messageBuilder(S2CLockDataFullPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CLockDataFullPacket::encode)
                .decoder(S2CLockDataFullPacket::decode)
                .consumerMainThread(S2CLockDataFullPacket::handle)
                .add();

        // S2C: Delta lock data sync (stages changed)
        CHANNEL.messageBuilder(S2CLockDataDeltaPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CLockDataDeltaPacket::encode)
                .decoder(S2CLockDataDeltaPacket::decode)
                .consumerMainThread(S2CLockDataDeltaPacket::handle)
                .add();

        // C2S: Request lock data refresh
        CHANNEL.messageBuilder(C2SRequestLockDataPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestLockDataPacket::encode)
                .decoder(C2SRequestLockDataPacket::decode)
                .consumerMainThread(C2SRequestLockDataPacket::handle)
                .add();

        EmiGameStagesLink.LOGGER.info("Network packets registered");
    }

    /**
     * Send full lock data to a specific player
     */
    public static void sendFullLockData(ServerPlayer player, LockSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CLockDataFullPacket(snapshot));
    }

    /**
     * Send delta lock data (stages changed) to a specific player
     */
    public static void sendDeltaLockData(ServerPlayer player, LockSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CLockDataDeltaPacket(snapshot));
    }

    /**
     * Request lock data from server (client-side call)
     */
    public static void requestLockData() {
        CHANNEL.sendToServer(new C2SRequestLockDataPacket());
    }
}
