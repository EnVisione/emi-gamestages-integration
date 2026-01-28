package com.enviouse.emi_gamestages_link.client.emi;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI Plugin for GameStages integration.
 * Registers with EMI to enable lock icon overlays on locked items.
 */
@EmiEntrypoint
public class EmiGameStagesPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        EmiGameStagesLink.LOGGER.info("EMI GameStages Integration plugin registered");
    }
}
