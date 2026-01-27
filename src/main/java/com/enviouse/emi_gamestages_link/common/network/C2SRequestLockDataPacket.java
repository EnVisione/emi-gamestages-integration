package com.enviouse.emi_gamestages_link.common.network;

import com.enviouse.emi_gamestages_link.server.LockDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to Server packet: Request lock data refresh.
 * Used when client wants to refresh EMI data (e.g., on EMI reload).
 */
public class C2SRequestLockDataPacket {

    public C2SRequestLockDataPacket() {
    }

    public static void encode(C2SRequestLockDataPacket packet, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static C2SRequestLockDataPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestLockDataPacket();
    }

    public static void handle(C2SRequestLockDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                LockDataManager.sendFullLockDataToPlayer(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
