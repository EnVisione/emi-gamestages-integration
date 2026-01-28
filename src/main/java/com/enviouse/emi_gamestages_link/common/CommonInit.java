package com.enviouse.emi_gamestages_link.common;

import com.enviouse.emi_gamestages_link.common.network.NetworkHandler;
import com.enviouse.emi_gamestages_link.common.integration.IntegrationManager;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Common initialization for both client and server.
 * Contains config registration, network setup, and shared registries.
 * This class is safe to load on both client and dedicated server.
 */
public class CommonInit {

    /**
     * Initialize common components.
     * Call this from the main mod constructor.
     */
    public static void init() {
        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfiguration.SPEC);

        // Register network packets
        NetworkHandler.register();

        // Initialize integration manager to detect available mods
        IntegrationManager.initialize();

        EmiGameStagesLink.LOGGER.info("EMI GameStages Link common initialization complete");
    }
}
