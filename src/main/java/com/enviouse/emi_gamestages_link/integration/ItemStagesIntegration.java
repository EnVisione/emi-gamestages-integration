package com.enviouse.emi_gamestages_link.integration;

import com.enviouse.emi_gamestages_link.EmiGameStagesLink;
import net.darkhax.itemstages.Restriction;
import net.darkhax.itemstages.RestrictionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

/**
 * Integration with ItemStages mod.
 * Provides automatic detection of items locked by ItemStages restrictions.
 */
public class ItemStagesIntegration {

    /**
     * Gets the required stage for an item from ItemStages.
     *
     * @param stack The ItemStack to check
     * @return The required stage name, or empty if not locked
     */
    public static Optional<String> getRequiredStage(ItemStack stack) {
        if (!IntegrationManager.isItemStagesLoaded() || stack.isEmpty()) {
            return Optional.empty();
        }
        try {
            return getRequiredStageInternal(stack);
        } catch (Exception e) {
            EmiGameStagesLink.LOGGER.debug("Error checking ItemStages: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> getRequiredStageInternal(ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
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
