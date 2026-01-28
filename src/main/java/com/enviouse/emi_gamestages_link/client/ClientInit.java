package com.enviouse.emi_gamestages_link.client;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.common.ModConfiguration;
import com.enviouse.emi_gamestages_link.common.network.NetworkHandler;
import com.enviouse.emi_gamestages_link.common.integration.IntegrationManager;
import net.darkhax.gamestages.event.StagesSyncedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Client-side initialization.
 * This class is ONLY loaded on the client via DistExecutor.
 * It contains all client-only references (Minecraft, LocalPlayer, EMI hooks, etc.)
 */
public class ClientInit {

    /**
     * Initialize client-side components.
     * Called via DistExecutor.safeRunWhenOn from the main mod constructor.
     */
    public static void init() {
        // Register client setup event on mod event bus
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ClientInit::onClientSetup);

        // Register client game events on forge event bus
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        EmiGameStagesLink.LOGGER.info("Client initialization registered");
    }

    /**
     * Client setup event handler
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        EmiGameStagesLink.LOGGER.info("EMI GameStages Link client setup complete");

        // Log integration status
        if (IntegrationManager.isItemStagesLoaded()) {
            EmiGameStagesLink.LOGGER.info("ItemStages integration active - item locks will be detected automatically");
        }
        if (IntegrationManager.isRecipeStagesLoaded()) {
            EmiGameStagesLink.LOGGER.info("RecipeStages integration active - recipe locks will be detected automatically");
        }
    }

    /**
     * Trigger EMI to refresh/reload its data.
     * Called when lock data is received from the server.
     */
    public static void triggerEmiRefresh() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    try {
                        dev.emi.emi.runtime.EmiReloadManager.reload();
                        EmiGameStagesLink.LOGGER.debug("Triggered EMI reload after lock data update");
                    } catch (Throwable t) {
                        EmiGameStagesLink.LOGGER.debug("Unable to trigger EMI reload: {}", t.getMessage());
                    }
                });
            }
        } catch (Throwable t) {
            EmiGameStagesLink.LOGGER.debug("Unable to schedule EMI reload: {}", t.getMessage());
        }
    }

    /**
     * Request fresh lock data from the server.
     * Can be called when EMI is opened or manually refreshed.
     */
    public static void requestLockDataRefresh() {
        try {
            NetworkHandler.requestLockData();
            EmiGameStagesLink.LOGGER.debug("Requested lock data refresh from server");
        } catch (Throwable t) {
            EmiGameStagesLink.LOGGER.debug("Unable to request lock data: {}", t.getMessage());
        }
    }

    /**
     * Client-side event handler
     */
    public static class ClientEventHandler {

        /**
         * Add lock info to item tooltips
         */
        @SubscribeEvent
        public void onItemTooltip(ItemTooltipEvent event) {
            if (!ModConfiguration.showTooltipInfo) {
                return;
            }

            var stack = event.getItemStack();
            if (stack.isEmpty()) {
                return;
            }

            // Use the new client-side lock resolver
            ClientStageLockResolver.LockInfo lockInfo = ClientStageLockResolver.getLockInfo(stack);
            if (lockInfo != null && lockInfo.isLocked()) {
                // Insert mod id line after the existing item name
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                String modId = id == null ? "unknown" : id.getNamespace();

                // Add the mod id line (darker gray)
                event.getToolTip().add(
                        Component.literal(modId)
                                .withStyle(ChatFormatting.DARK_GRAY)
                );

                // Add the locked indicator in the configured orange
                int rgb = ModConfiguration.highlightColor & 0xFFFFFF;
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
         * Clear cache when disconnecting from server
         */
        @SubscribeEvent
        public void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            LockStateCache.clear();
            EmiGameStagesLink.LOGGER.debug("Cleared lock state cache on disconnect");
        }

        /**
         * Handle GameStages sync event on client.
         * This provides backwards compatibility and triggers EMI refresh
         * when stages are synced from the server.
         */
        @SubscribeEvent
        public void onStagesSynced(StagesSyncedEvent event) {
            // Only reload EMI if our visuals are enabled
            if (!ModConfiguration.showLockIcon && !ModConfiguration.showTooltipInfo && !ModConfiguration.showLockOnItemList) {
                return;
            }

            // Clear local cache to force re-check
            ClientStageLockResolver.clearCache();

            // Trigger EMI refresh after a short delay to give JEI plugins time to update
            try {
                final int delayMs = 600;
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {}
                    triggerEmiRefresh();
                }, "EmiReload-Delayed-For-StageSync");
                t.setDaemon(true);
                t.start();
            } catch (Throwable t) {
                EmiGameStagesLink.LOGGER.debug("Unable to schedule EMI reload on stage sync: {}", t.getMessage());
            }
        }
    }
}
