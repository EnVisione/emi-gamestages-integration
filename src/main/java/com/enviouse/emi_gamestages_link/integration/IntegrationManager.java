package com.enviouse.emi_gamestages_link.integration;

import com.enviouse.emi_gamestages_link.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.ModConfiguration;
import net.minecraftforge.fml.ModList;

/**
 * Manages integration with optional mods like ItemStages and RecipeStages.
 * Detects which mods are present at runtime and enables their integrations.
 */
public class IntegrationManager {

    private static boolean initialized = false;
    private static boolean itemStagesPresent = false;
    private static boolean recipeStagesPresent = false;

    /**
     * Initialize the integration manager and detect available mods.
     * Should be called during mod setup.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        // Check for ItemStages
        itemStagesPresent = ModList.get().isLoaded("itemstages");
        if (itemStagesPresent) {
            EmiGameStagesLink.LOGGER.info("ItemStages detected - automatic item stage integration available");
        }

        // Check for RecipeStages
        recipeStagesPresent = ModList.get().isLoaded("recipestages");
        if (recipeStagesPresent) {
            EmiGameStagesLink.LOGGER.info("RecipeStages detected - automatic recipe stage integration available");
        }

        if (!itemStagesPresent && !recipeStagesPresent) {
            EmiGameStagesLink.LOGGER.info("No stage mods detected - using manual config only");
        }

        initialized = true;
    }

    /**
     * @return true if ItemStages mod is loaded AND integration is enabled in config
     */
    public static boolean isItemStagesLoaded() {
        if (!initialized) {
            initialize();
        }
        return itemStagesPresent && ModConfiguration.enableItemStagesIntegration;
    }

    /**
     * @return true if RecipeStages mod is loaded AND integration is enabled in config
     */
    public static boolean isRecipeStagesLoaded() {
        if (!initialized) {
            initialize();
        }
        return recipeStagesPresent && ModConfiguration.enableRecipeStagesIntegration;
    }
}
