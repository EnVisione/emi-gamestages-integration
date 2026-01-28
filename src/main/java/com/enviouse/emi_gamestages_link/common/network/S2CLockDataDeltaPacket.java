package com.enviouse.emi_gamestages_link.common.network;

import com.enviouse.emi_gamestages_link.common.LockSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to Client packet: Delta lock data synchronization.
 * Sent when a player's stages change (add/remove/clear).
 */
public class S2CLockDataDeltaPacket {

    private final LockSnapshot snapshot;

    public S2CLockDataDeltaPacket(LockSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(S2CLockDataDeltaPacket packet, FriendlyByteBuf buf) {
        packet.snapshot.toNetwork(buf);
    }

    public static S2CLockDataDeltaPacket decode(FriendlyByteBuf buf) {
        return new S2CLockDataDeltaPacket(LockSnapshot.fromNetwork(buf));
    }

    public static void handle(S2CLockDataDeltaPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client - use DistExecutor to safely reference client code
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.enviouse.emi_gamestages_link.client.ClientPacketHandler.handleDeltaLockData(packet.snapshot);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public LockSnapshot getSnapshot() {
        return snapshot;
    }
}
