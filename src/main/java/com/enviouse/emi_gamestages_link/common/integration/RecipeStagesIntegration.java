package com.enviouse.emi_gamestages_link.common.integration;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Optional;

/**
 * Integration with RecipeStages mod.
 * Provides automatic detection of recipes locked by RecipeStages.
 * This class is server-safe - no client imports.
 */
public class RecipeStagesIntegration {

    /**
     * Gets the required stage for a recipe from RecipeStages.
     *
     * @param recipe The Minecraft recipe to check
     * @return The required stage name, or empty if not staged
     */
    public static Optional<String> getRequiredStage(Recipe<?> recipe) {
        if (!IntegrationManager.isRecipeStagesLoaded() || recipe == null) {
            return Optional.empty();
        }
        try {
            return getRequiredStageInternal(recipe);
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error checking RecipeStages: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> getRequiredStageInternal(Recipe<?> recipe) {
        if (recipe.getClass().getName().contains("recipestages") ||
            implementsIStagedRecipe(recipe)) {
            try {
                var method = recipe.getClass().getMethod("getStage");
                Object result = method.invoke(recipe);
                if (result instanceof String stage) {
                    return Optional.of(stage);
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private static boolean implementsIStagedRecipe(Recipe<?> recipe) {
        for (Class<?> iface : recipe.getClass().getInterfaces()) {
            if (iface.getSimpleName().equals("IStagedRecipe")) {
                return true;
            }
        }
        return false;
    }


    public record RecipeStageLockInfo(String stageName, boolean isLocked) {}
}
