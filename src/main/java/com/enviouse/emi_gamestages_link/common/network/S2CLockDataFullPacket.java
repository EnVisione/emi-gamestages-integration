package com.enviouse.emi_gamestages_link.common.network;

import com.enviouse.emi_gamestages_link.common.LockSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to Client packet: Full lock data synchronization.
 * Sent on login, datapack reload, or when client requests refresh.
 */
public class S2CLockDataFullPacket {

    private final LockSnapshot snapshot;

    public S2CLockDataFullPacket(LockSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(S2CLockDataFullPacket packet, FriendlyByteBuf buf) {
        packet.snapshot.toNetwork(buf);
    }

    public static S2CLockDataFullPacket decode(FriendlyByteBuf buf) {
        return new S2CLockDataFullPacket(LockSnapshot.fromNetwork(buf));
    }

    public static void handle(S2CLockDataFullPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on client - use DistExecutor to safely reference client code
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.enviouse.emi_gamestages_link.client.ClientPacketHandler.handleFullLockData(packet.snapshot);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public LockSnapshot getSnapshot() {
        return snapshot;
    }
}
