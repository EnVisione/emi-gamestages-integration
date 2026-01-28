package com.enviouse.emi_gamestages_link.client;

import com.enviouse.emi_gamestages_link.common.LockSnapshot;

/**
 * Client-side packet handler.
 * This class is ONLY loaded on the client via DistExecutor usage in packet handlers.
 * It references client-only code safely.
 */
public class ClientPacketHandler {

    /**
     * Handle full lock data sync from server
     */
    public static void handleFullLockData(LockSnapshot snapshot) {
        LockStateCache.setSnapshot(snapshot);
        ClientInit.triggerEmiRefresh();
    }

    /**
     * Handle delta lock data sync from server (stages changed)
     */
    public static void handleDeltaLockData(LockSnapshot snapshot) {
        LockStateCache.setSnapshot(snapshot);
        ClientInit.triggerEmiRefresh();
    }
}
