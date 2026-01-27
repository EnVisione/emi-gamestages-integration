package com.enviouse.emi_gamestages_link;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Helper class for rendering lock overlays.
 */
public class LockRenderHelper {

    // Lock icon texture
    public static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath(EmiGameStagesLink.MODID, "textures/gui/pixellock.png");

    /**
     * Draws the lock icon at the specified position.
     * Renders on top of everything by using flush, high z-level, and disabled depth test.
     *
     * @param graphics The GuiGraphics context
     * @param x The X position
     * @param y The Y position
     */
    public static void drawLockIconAt(GuiGraphics graphics, int x, int y) {
        if (!ModConfiguration.showLockIcon) {
            return;
        }

        int iconSize = ModConfiguration.iconSize;

        // Flush any pending renders to ensure our icon draws on top
        graphics.flush();

        // Save current state
        graphics.pose().pushPose();

        // Translate to high z-level
        graphics.pose().translate(0, 0, 1000);

        // Disable depth test to render on top
        RenderSystem.disableDepthTest();

        // Enable blending for transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Bind the texture for the blit call
        RenderSystem.setShaderTexture(0, LOCK_ICON);

        // Draw the lock icon (uses the GuiGraphics blit which expects texture size parameters)
        graphics.blit(LOCK_ICON, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // Restore state
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();

        graphics.pose().popPose();

        // Flush again to make sure we don't interfere with subsequent batched renders
        graphics.flush();
    }

    /**
     * Draws a highlight overlay on a slot to indicate it's locked.
     * Uses semi-transparent orange by default.
     *
     * @param graphics The GuiGraphics context
     * @param x The X position
     * @param y The Y position
     * @param width The width
     * @param height The height
     */
    public static void drawLockedHighlight(GuiGraphics graphics, int x, int y, int width, int height) {
        if (!ModConfiguration.highlightLockedOutput) {
            return;
        }

        int color = ModConfiguration.highlightColor;
        graphics.fill(x, y, x + width, y + height, color);
    }
}
