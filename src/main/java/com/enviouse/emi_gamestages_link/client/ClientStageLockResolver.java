package com.enviouse.emi_gamestages_link.client;

import com.enviouse.emi_gamestages_link.common.ModConfiguration;
import com.enviouse.emi_gamestages_link.common.LockEntry;
import com.enviouse.emi_gamestages_link.common.integration.IntegrationManager;
import com.enviouse.emi_gamestages_link.common.integration.ItemStagesIntegration;
import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side stage lock resolver.
 * Uses server-synced data from LockStateCache when available,
 * with fallback to local detection for immediate responsiveness.
 */
public class ClientStageLockResolver {

    // Cache for config-based lock resolutions (fallback)
    private static final Map<String, Optional<String>> localCache = new ConcurrentHashMap<>();
    private static long lastCacheInvalidation = 0;
    private static final long CACHE_TTL = 5000;

    /**
     * Gets the required stage for an ItemStack.
     * First checks server-synced data, then falls back to local detection.
     */
    public static Optional<String> getRequiredStage(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        // Check server-synced lock data first
        LockEntry serverEntry = LockStateCache.getItemLockEntry(itemId);
        if (serverEntry != null) {
            return Optional.of(serverEntry.requiredStage());
        }

        // Fallback to ItemStages local detection (for immediate responsiveness)
        if (IntegrationManager.isItemStagesLoaded()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Optional<String> itemStagesResult = ItemStagesIntegration.getRequiredStage(player, stack);
                if (itemStagesResult.isPresent()) {
                    return itemStagesResult;
                }
            }
        }

        // Fallback to config-based lookups
        invalidateCacheIfNeeded();
        return localCache.computeIfAbsent(itemId.toString(), id -> computeConfigStage(stack, id));
    }

    /**
     * Checks if an ItemStack is locked for the current player.
     */
    public static boolean isLocked(ItemStack stack) {
        Optional<String> requiredStage = getRequiredStage(stack);
        if (requiredStage.isEmpty()) {
            return false;
        }

        // Check server-synced stage data first
        if (LockStateCache.hasStage(requiredStage.get())) {
            return false; // Player has the stage
        }

        // Fallback to local GameStageHelper check
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return true; // No player, assume locked
        }

        return !GameStageHelper.hasStage(player, requiredStage.get());
    }

    /**
     * Gets the lock info for an item (for display purposes).
     */
    @Nullable
    public static LockInfo getLockInfo(ItemStack stack) {
        Optional<String> requiredStage = getRequiredStage(stack);
        if (requiredStage.isEmpty()) {
            return null;
        }

        boolean isLocked = !LockStateCache.hasStage(requiredStage.get());

        // Double-check with local data if we have a player
        Player player = Minecraft.getInstance().player;
        if (player != null && !isLocked) {
            isLocked = !GameStageHelper.hasStage(player, requiredStage.get());
        }

        return new LockInfo(requiredStage.get(), isLocked);
    }

    private static void invalidateCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheInvalidation > CACHE_TTL) {
            localCache.clear();
            lastCacheInvalidation = now;
        }
    }


    private static Optional<String> computeConfigStage(ItemStack stack, String itemId) {
        // Check direct item locks from config
        if (ModConfiguration.itemLocks.containsKey(itemId)) {
            return Optional.of(ModConfiguration.itemLocks.get(itemId));
        }

        // Check tag locks from config
        for (Map.Entry<String, String> entry : ModConfiguration.tagLocks.entrySet()) {
            String tagStr = entry.getKey();
            if (tagStr.startsWith("#")) {
                tagStr = tagStr.substring(1);
            }

            ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
            if (tagId != null) {
                TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);
                if (stack.is(tagKey)) {
                    return Optional.of(entry.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Clear the local cache (called when server data is received)
     */
    public static void clearCache() {
        localCache.clear();
        lastCacheInvalidation = System.currentTimeMillis();
    }

    /**
     * Information about a lock on an item.
     */
    public record LockInfo(String stageName, boolean isLocked) {}
}
