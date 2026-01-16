package com.enviouse.emi_gamestages_link;

import com.enviouse.emi_gamestages_link.integration.IntegrationManager;
import com.enviouse.emi_gamestages_link.integration.ItemStagesIntegration;
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
 * Resolves stage locks for items.
 * Checks ItemStages integration first, then falls back to config-based locks.
 */
public class StageLockResolver {

    // Cache for config-based lock resolutions
    private static final Map<String, Optional<String>> lockCache = new ConcurrentHashMap<>();
    private static long lastCacheInvalidation = 0;
    private static final long CACHE_TTL = 5000; // 5 seconds

    /**
     * Gets the required stage for an ItemStack.
     *
     * @param stack The ItemStack to check
     * @return The required stage name, or empty if not locked
     */
    public static Optional<String> getRequiredStage(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        // Check ItemStages first (not cached because it's player-specific)
        if (IntegrationManager.isItemStagesLoaded()) {
            Optional<String> itemStagesResult = ItemStagesIntegration.getRequiredStage(stack);
            if (itemStagesResult.isPresent()) {
                return itemStagesResult;
            }
        }

        // Fall back to config-based lookups (cached)
        invalidateCacheIfNeeded();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }

        return lockCache.computeIfAbsent(itemId.toString(), id -> computeConfigStage(stack, id));
    }

    /**
     * Checks if an ItemStack is locked for the current player.
     *
     * @param stack The ItemStack to check
     * @return true if the item is locked
     */
    public static boolean isLocked(ItemStack stack) {
        Optional<String> requiredStage = getRequiredStage(stack);
        if (requiredStage.isEmpty()) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return true; // No player, assume locked
        }

        return !GameStageHelper.hasStage(player, requiredStage.get());
    }

    /**
     * Gets the lock info for an item (for display purposes).
     *
     * @param stack The ItemStack to check
     * @return LockInfo if locked, null otherwise
     */
    @Nullable
    public static LockInfo getLockInfo(ItemStack stack) {
        Optional<String> requiredStage = getRequiredStage(stack);
        if (requiredStage.isEmpty()) {
            return null;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return new LockInfo(requiredStage.get(), true);
        }

        boolean isLocked = !GameStageHelper.hasStage(player, requiredStage.get());
        return new LockInfo(requiredStage.get(), isLocked);
    }

    private static void invalidateCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheInvalidation > CACHE_TTL) {
            lockCache.clear();
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
     * Information about a lock on an item.
     */
    public record LockInfo(String stageName, boolean isLocked) {}
}
