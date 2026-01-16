package com.enviouse.emi_gamestages_link;

import com.enviouse.emi_gamestages_link.integration.IntegrationManager;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * EMI GameStages Link - Integration mod between EMI and GameStages.
 * Shows lock overlays on items/recipes that are locked by GameStages,
 * ItemStages, or RecipeStages.
 *
 * Automatic integration with:
 * - ItemStages: Detects item restrictions automatically
 * - RecipeStages: Detects recipe restrictions automatically
 * - Manual config: Supports custom item/tag -> stage mappings
 */
@Mod(EmiGameStagesLink.MODID)
@SuppressWarnings("removal")
public class EmiGameStagesLink {

    public static final String MODID = "emi_gamestages_link";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmiGameStagesLink() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register client setup event
        modEventBus.addListener(this::onClientSetup);

        // Register ourselves for game events (including tooltip events)
        MinecraftForge.EVENT_BUS.register(this);

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfiguration.SPEC);

        // Initialize integration manager to detect available mods
        IntegrationManager.initialize();

        LOGGER.info("EMI GameStages Link initialized");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("EMI GameStages Link client setup complete");

        // Log integration status
        if (IntegrationManager.isItemStagesLoaded()) {
            LOGGER.info("ItemStages integration active - item locks will be detected automatically");
        }
        if (IntegrationManager.isRecipeStagesLoaded()) {
            LOGGER.info("RecipeStages integration active - recipe locks will be detected automatically");
        }
    }

    /**
     * Adds locked stage information to item tooltips.
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ModConfiguration.showTooltipInfo) {
            return;
        }

        StageLockResolver.LockInfo lockInfo = StageLockResolver.getLockInfo(event.getItemStack());
        if (lockInfo != null && lockInfo.isLocked()) {
            // Add a blank line for spacing
            event.getToolTip().add(Component.empty());

            // Add the locked indicator in orange/gold
            event.getToolTip().add(
                Component.literal("🔒 This item is locked")
                    .withStyle(ChatFormatting.GOLD)
            );

            // Add the stage name in gray
            event.getToolTip().add(
                Component.literal("Stage required: " + lockInfo.stageName())
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }
}

