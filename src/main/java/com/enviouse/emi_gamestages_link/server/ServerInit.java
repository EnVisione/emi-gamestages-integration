package com.enviouse.emi_gamestages_link.server;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import net.darkhax.gamestages.event.GameStageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Server-side initialization and event handling.
 * This class is safe to load on dedicated servers - no client imports.
 */
public class ServerInit {

    /**
     * Initialize server-side components.
     * Call this from the main mod constructor.
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
        EmiGameStagesLink.LOGGER.info("Server event handlers registered");
    }

    /**
     * Server-side event handler class
     */
    public static class ServerEventHandler {

        /**
         * Send lock data when player logs in
         */
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                // Delay slightly to ensure player is fully initialized
                serverPlayer.getServer().execute(() -> {
                    LockDataManager.sendFullLockDataToPlayer(serverPlayer);
                    EmiGameStagesLink.LOGGER.debug("Sent initial lock data to {}", serverPlayer.getName().getString());
                });
            }
        }

        /**
         * Send lock data when datapacks are reloaded
         */
        @SubscribeEvent
        public void onDatapackSync(OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                // Single player sync
                LockDataManager.sendFullLockDataToPlayer(event.getPlayer());
            } else if (event.getPlayerList() != null) {
                // All players sync (e.g., /reload command)
                for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                    LockDataManager.sendFullLockDataToPlayer(player);
                }
                EmiGameStagesLink.LOGGER.debug("Sent lock data to all players after datapack reload");
            }
        }

        /**
         * Send delta lock data when a stage is added
         */
        @SubscribeEvent
        public void onStageAdded(GameStageEvent.Added event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                LockDataManager.sendDeltaLockDataToPlayer(serverPlayer);
                EmiGameStagesLink.LOGGER.debug("Sent delta lock data to {} after stage added: {}",
                        serverPlayer.getName().getString(), event.getStageName());
            }
        }

        /**
         * Send delta lock data when a stage is removed
         */
        @SubscribeEvent
        public void onStageRemoved(GameStageEvent.Removed event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                LockDataManager.sendDeltaLockDataToPlayer(serverPlayer);
                EmiGameStagesLink.LOGGER.debug("Sent delta lock data to {} after stage removed: {}",
                        serverPlayer.getName().getString(), event.getStageName());
            }
        }

        /**
         * Send delta lock data when stages are cleared
         */
        @SubscribeEvent
        public void onStagesCleared(GameStageEvent.Cleared event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                LockDataManager.sendDeltaLockDataToPlayer(serverPlayer);
                EmiGameStagesLink.LOGGER.debug("Sent delta lock data to {} after stages cleared",
                        serverPlayer.getName().getString());
            }
        }
    }
}
