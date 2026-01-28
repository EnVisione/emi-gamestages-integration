package com.enviouse.emi_gamestages_link.common;

import com.enviouse.emi_gamestages_link.server.ServerInit;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
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
 *
 * Architecture:
 * - Common: Config, network packets, shared data types (safe for both sides)
 * - Server: Lock computation, server event listeners (safe for dedicated server)
 * - Client: EMI hooks, overlays, tooltips, client cache (client-only via DistExecutor)
 */
@Mod(EmiGameStagesLink.MODID)
@SuppressWarnings("removal")
public class EmiGameStagesLink {

    public static final String MODID = "emi_gamestages_link";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmiGameStagesLink() {
        // Initialize common components (config, network, shared registries)
        // Safe on both client and dedicated server
        CommonInit.init();

        // Initialize server components (server event listeners)
        // Safe on both physical sides - handles logical server events
        ServerInit.init();

        // Initialize client components ONLY on physical client
        // Uses DistExecutor.safeRunWhenOn to prevent classloading on server
        // Note: We use a lambda that references the class only when executed
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> com.enviouse.emi_gamestages_link.client.ClientInit::init);

        LOGGER.info("EMI GameStages Link initialized");
    }
}
