package com.enviouse.emi_gamestages_link.server;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.common.ModConfiguration;
import com.enviouse.emi_gamestages_link.common.LockEntry;
import com.enviouse.emi_gamestages_link.common.LockSnapshot;
import com.enviouse.emi_gamestages_link.common.LockType;
import com.enviouse.emi_gamestages_link.common.network.NetworkHandler;
import com.enviouse.emi_gamestages_link.common.integration.IntegrationManager;
import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.gamestages.data.IStageData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Server-side manager for computing and sending lock data to clients.
 * This class is safe to load on dedicated servers.
 */
public class LockDataManager {

    /**
     * Build a complete lock snapshot for a player.
     * This includes all locked items and recipes from all sources.
     */
    public static LockSnapshot buildSnapshot(ServerPlayer player) {
        LockSnapshot snapshot = new LockSnapshot();

        // Get player's unlocked stages
        IStageData stageData = GameStageHelper.getPlayerData(player);
        if (stageData != null) {
            Collection<String> stages = stageData.getStages();
            snapshot.setUnlockedStages(stages);
        }

        // Add ItemStages locks if available
        if (IntegrationManager.isItemStagesLoaded()) {
            addItemStagesLocks(snapshot, player);
        }

        // Add RecipeStages locks if available
        if (IntegrationManager.isRecipeStagesLoaded()) {
            addRecipeStagesLocks(snapshot, player);
        }

        // Add config-based locks
        addConfigLocks(snapshot);

        EmiGameStagesLink.LOGGER.debug("Built lock snapshot for {}: {} item locks, {} recipe locks, {} unlocked stages",
                player.getName().getString(),
                snapshot.getItemLocks().size(),
                snapshot.getRecipeLocks().size(),
                snapshot.getUnlockedStages().size());

        return snapshot;
    }

    /**
     * Send full lock data to a player
     */
    public static void sendFullLockDataToPlayer(ServerPlayer player) {
        LockSnapshot snapshot = buildSnapshot(player);
        NetworkHandler.sendFullLockData(player, snapshot);
    }

    /**
     * Send delta lock data to a player (after stage change)
     */
    public static void sendDeltaLockDataToPlayer(ServerPlayer player) {
        LockSnapshot snapshot = buildSnapshot(player);
        NetworkHandler.sendDeltaLockData(player, snapshot);
    }

    private static void addItemStagesLocks(LockSnapshot snapshot, ServerPlayer player) {
        try {
            // Use reflection to access ItemStages RestrictionManager
            if (!ModList.get().isLoaded("itemstages")) {
                return;
            }

            Class<?> restrictionManagerClass = Class.forName("net.darkhax.itemstages.RestrictionManager");
            Object instance = restrictionManagerClass.getField("INSTANCE").get(null);

            // Get all restrictions
            var getAllRestrictionsMethod = restrictionManagerClass.getMethod("getAllRestrictions");
            @SuppressWarnings("unchecked")
            Collection<Object> restrictions = (Collection<Object>) getAllRestrictionsMethod.invoke(instance);

            for (Object restriction : restrictions) {
                try {
                    // Get stages from restriction
                    var getStagesMethod = restriction.getClass().getMethod("getStages");
                    @SuppressWarnings("unchecked")
                    Set<String> stages = (Set<String>) getStagesMethod.invoke(restriction);

                    if (stages == null || stages.isEmpty()) {
                        continue;
                    }

                    String requiredStage = stages.iterator().next();

                    // Get item predicate and test against all items
                    var getItemPredicateMethod = restriction.getClass().getMethod("getItemPredicate");
                    Object predicate = getItemPredicateMethod.invoke(restriction);

                    if (predicate != null) {
                        // We can't easily iterate predicates, so we'll rely on config and client-side detection
                        // For now, just log that we found restrictions
                    }
                } catch (Exception e) {
                    EmiGameStagesLink.LOGGER.debug("Error processing ItemStages restriction: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error accessing ItemStages: {}", e.getMessage());
        }
    }

    private static void addRecipeStagesLocks(LockSnapshot snapshot, ServerPlayer player) {
        try {
            if (!ModList.get().isLoaded("recipestages")) {
                return;
            }

            // Get recipe manager and iterate staged recipes
            var level = player.level();
            var recipeManager = level.getRecipeManager();

            for (var recipe : recipeManager.getRecipes()) {
                try {
                    // Check if recipe implements IStagedRecipe
                    for (Class<?> iface : recipe.getClass().getInterfaces()) {
                        if (iface.getSimpleName().equals("IStagedRecipe")) {
                            var getStageMethod = recipe.getClass().getMethod("getStage");
                            Object result = getStageMethod.invoke(recipe);
                            if (result instanceof String stage && !stage.isEmpty()) {
                                ResourceLocation recipeId = recipe.getId();
                                LockEntry entry = new LockEntry(
                                        recipeId,
                                        LockEntry.EntryType.RECIPE,
                                        stage,
                                        LockType.RECIPE_STAGES,
                                        "RecipeStages"
                                );
                                snapshot.addRecipeLock(entry);
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore individual recipe errors
                }
            }
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error accessing RecipeStages: {}", e.getMessage());
        }
    }

    private static void addConfigLocks(LockSnapshot snapshot) {
        // Add direct item locks from config
        for (Map.Entry<String, String> entry : ModConfiguration.itemLocks.entrySet()) {
            ResourceLocation itemId = ResourceLocation.tryParse(entry.getKey());
            if (itemId != null) {
                LockEntry lockEntry = new LockEntry(
                        itemId,
                        LockEntry.EntryType.ITEM,
                        entry.getValue(),
                        LockType.CONFIG,
                        "Config"
                );
                snapshot.addItemLock(lockEntry);
            }
        }

        // Add tag-based locks from config
        for (Map.Entry<String, String> entry : ModConfiguration.tagLocks.entrySet()) {
            String tagStr = entry.getKey();
            if (tagStr.startsWith("#")) {
                tagStr = tagStr.substring(1);
            }

            ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
            if (tagId == null) {
                continue;
            }

            TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId);

            // Find all items with this tag
            for (Item item : ForgeRegistries.ITEMS) {
                ItemStack stack = new ItemStack(item);
                if (stack.is(tagKey)) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                    if (itemId != null) {
                        LockEntry lockEntry = new LockEntry(
                                itemId,
                                LockEntry.EntryType.ITEM,
                                entry.getValue(),
                                LockType.CONFIG,
                                "Config (tag: " + entry.getKey() + ")"
                        );
                        snapshot.addItemLock(lockEntry);
                    }
                }
            }
        }
    }
}
