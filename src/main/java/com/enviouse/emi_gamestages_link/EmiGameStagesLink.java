package com.enviouse.emi_gamestages_link;

import com.enviouse.emi_gamestages_link.integration.IntegrationManager;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.darkhax.gamestages.event.StagesSyncedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.Minecraft;

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

        var stack = event.getItemStack();
        StageLockResolver.LockInfo lockInfo = StageLockResolver.getLockInfo(stack);
        if (lockInfo != null && lockInfo.isLocked()) {
            // Insert mod id line after the existing item name
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            String modId = id == null ? "unknown" : id.getNamespace();

            // Add the mod id line (darker gray)
            event.getToolTip().add(
                Component.literal(modId)
                    .withStyle(ChatFormatting.DARK_GRAY)
            );

            // Add the locked indicator in the configured orange (uses highlightColor RGB)
            int rgb = ModConfiguration.highlightColor & 0xFFFFFF; // drop alpha for text color
            event.getToolTip().add(
                Component.literal("🔒 This item is locked")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
            );

            // Add the stage name in gray
            event.getToolTip().add(
                Component.literal("Stage required: " + lockInfo.stageName())
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    /**
     * Listen for GameStages sync events and trigger EMI reload so overlays update.
     */
    @SubscribeEvent
    public void onStagesSynced(StagesSyncedEvent event) {
        // Only reload EMI if our visuals are enabled
        if (!ModConfiguration.showLockIcon && !ModConfiguration.showTooltipInfo && !ModConfiguration.showLockOnItemList) {
            return;
        }

        // Schedule a reload after a short delay so JEI-based re-adds have time to run first
        try {
            final int delayMs = 600; // give JEI plugin time to restore hidden ingredients
            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {}
                try {
                    // Run on client thread if possible
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.execute(() -> dev.emi.emi.runtime.EmiReloadManager.reload());
                    } else {
                        dev.emi.emi.runtime.EmiReloadManager.reload();
                    }
                    LOGGER.debug("Triggered delayed EMI reload due to stage sync");
                } catch (Throwable t) {
                    LOGGER.debug("Unable to trigger EMI reload on stage sync: {}", t.getMessage());
                }
            }, "EmiReload-Delayed-For-JEI-Readd").start();
        } catch (Throwable t) {
            LOGGER.debug("Unable to schedule EMI reload on stage sync: {}", t.getMessage());
        }
    }
}
