package com.enviouse.emi_gamestages_link.client;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.common.LockEntry;
import com.enviouse.emi_gamestages_link.common.LockSnapshot;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Client-side cache for lock state data received from the server.
 * This class stores the server-authoritative lock data for display purposes.
 */
public class LockStateCache {

    private static volatile LockSnapshot currentSnapshot = new LockSnapshot();

    /**
     * Set the current snapshot (called when receiving data from server)
     */
    public static void setSnapshot(LockSnapshot snapshot) {
        currentSnapshot = snapshot;
        EmiGameStagesLink.LOGGER.debug("Updated lock state cache: {} item locks, {} recipe locks, {} unlocked stages",
                snapshot.getItemLocks().size(),
                snapshot.getRecipeLocks().size(),
                snapshot.getUnlockedStages().size());
    }

    /**
     * Get the current snapshot
     */
    public static LockSnapshot getSnapshot() {
        return currentSnapshot;
    }

    /**
     * Check if an item is locked
     */
    public static boolean isItemLocked(ResourceLocation itemId) {
        return currentSnapshot.isItemLocked(itemId);
    }

    /**
     * Check if a recipe is locked
     */
    public static boolean isRecipeLocked(ResourceLocation recipeId) {
        return currentSnapshot.isRecipeLocked(recipeId);
    }

    /**
     * Get lock info for an item
     */
    @Nullable
    public static LockEntry getItemLockEntry(ResourceLocation itemId) {
        return currentSnapshot.getItemLock(itemId);
    }

    /**
     * Get lock info for a recipe
     */
    @Nullable
    public static LockEntry getRecipeLockEntry(ResourceLocation recipeId) {
        return currentSnapshot.getRecipeLock(recipeId);
    }

    /**
     * Check if the player has a specific stage
     */
    public static boolean hasStage(String stage) {
        return currentSnapshot.hasStage(stage);
    }

    /**
     * Clear the cache (called on disconnect)
     */
    public static void clear() {
        currentSnapshot = new LockSnapshot();
    }
}
