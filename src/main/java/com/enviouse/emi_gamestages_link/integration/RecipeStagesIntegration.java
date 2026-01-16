package com.enviouse.emi_gamestages_link.integration;

import com.enviouse.emi_gamestages_link.EmiGameStagesLink;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Integration with RecipeStages mod.
 * Provides automatic detection of recipes locked by RecipeStages.
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

    /**
     * Gets lock info for a recipe.
     */
    @Nullable
    public static RecipeStageLockInfo getLockInfo(Recipe<?> recipe) {
        Optional<String> stage = getRequiredStage(recipe);
        if (stage.isEmpty()) {
            return null;
        }
        Player player = Minecraft.getInstance().player;
        boolean isLocked = player == null || !GameStageHelper.hasStage(player, stage.get());
        return new RecipeStageLockInfo(stage.get(), isLocked);
    }

    /**
     * Tries to get lock info from an EMI recipe by checking its backing recipe.
     */
    @Nullable
    public static RecipeStageLockInfo getLockInfoFromEmiRecipe(EmiRecipe emiRecipe) {
        if (!IntegrationManager.isRecipeStagesLoaded() || emiRecipe == null) {
            return null;
        }
        try {
            ResourceLocation recipeId = emiRecipe.getId();
            if (recipeId == null) {
                return null;
            }
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            var recipeManager = level.getRecipeManager();
            var optRecipe = recipeManager.byKey(recipeId);
            if (optRecipe.isPresent()) {
                return getLockInfo(optRecipe.get());
            }
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error getting recipe lock info: {}", e.getMessage());
        }
        return null;
    }

    public record RecipeStageLockInfo(String stageName, boolean isLocked) {}
}
