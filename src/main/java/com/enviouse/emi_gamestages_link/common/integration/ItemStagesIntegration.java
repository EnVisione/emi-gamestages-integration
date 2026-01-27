package com.enviouse.emi_gamestages_link.common.integration;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import net.darkhax.itemstages.Restriction;
import net.darkhax.itemstages.RestrictionManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

/**
 * Integration with ItemStages mod.
 * Provides automatic detection of items locked by ItemStages restrictions.
 * This class is server-safe - no client imports.
 */
public class ItemStagesIntegration {

    /**
     * Gets the required stage for an item from ItemStages for a specific player.
     *
     * @param player The player to check restrictions for
     * @param stack The ItemStack to check
     * @return The required stage name, or empty if not locked
     */
    public static Optional<String> getRequiredStage(Player player, ItemStack stack) {
        if (!IntegrationManager.isItemStagesLoaded() || stack.isEmpty() || player == null) {
            return Optional.empty();
        }
        try {
            return getRequiredStageInternal(player, stack);
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error checking ItemStages: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> getRequiredStageInternal(Player player, ItemStack stack) {
        Restriction restriction = RestrictionManager.INSTANCE.getRestriction(player, stack);
        if (restriction != null) {
            Set<String> stages = restriction.getStages();
            if (!stages.isEmpty()) {
                return Optional.of(stages.iterator().next());
            }
        }
        return Optional.empty();
    }
}
