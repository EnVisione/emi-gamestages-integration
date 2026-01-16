package com.enviouse.emi_gamestages_link.mixin;

import com.enviouse.emi_gamestages_link.LockRenderHelper;
import com.enviouse.emi_gamestages_link.ModConfiguration;
import com.enviouse.emi_gamestages_link.StageLockResolver;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add lock overlay rendering to EMI SlotWidget.
 * Handles lock rendering for recipe view slots.
 *
 * SlotWidget.render() calls: drawBackground() -> drawStack() -> drawOverlay()
 * We draw our lock in drawOverlay after the item has rendered.
 */
@Mixin(value = SlotWidget.class, remap = false)
public abstract class SlotWidgetMixin {

    @Shadow
    public abstract EmiIngredient getStack();

    @Shadow
    public abstract Bounds getBounds();


    /**
     * Draw lock overlay in drawOverlay (after the item has rendered).
     */
    @Inject(method = "drawOverlay", at = @At("TAIL"), require = 0)
    private void emi_gamestages_link$onDrawOverlayEnd(GuiGraphics draw, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        emi_gamestages_link$renderLockIfNeeded(draw);
    }

    @Unique
    private void emi_gamestages_link$renderLockIfNeeded(GuiGraphics draw) {
        EmiIngredient ingredient = getStack();
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }

        // Get the first EmiStack to check for lock
        for (EmiStack stack : ingredient.getEmiStacks()) {
            ItemStack itemStack = stack.getItemStack();
            if (itemStack.isEmpty()) {
                continue;
            }

            StageLockResolver.LockInfo lockInfo = StageLockResolver.getLockInfo(itemStack);
            if (lockInfo != null && lockInfo.isLocked()) {
                Bounds bounds = getBounds();

                // Draw light orange highlight on the slot
                if (ModConfiguration.highlightLockedOutput) {
                    LockRenderHelper.drawLockedHighlight(draw,
                            bounds.x() + 1, bounds.y() + 1,
                            bounds.width() - 2, bounds.height() - 2);
                }

                // Draw lock icon at top-left
                if (ModConfiguration.showLockIcon) {
                    int padding = ModConfiguration.iconPadding;
                    int iconX = bounds.x() + padding + 1;
                    int iconY = bounds.y() + padding + 1;
                    LockRenderHelper.drawLockIconAt(draw, iconX, iconY);
                }

                // Only process first stack
                break;
            }
        }
    }
}

