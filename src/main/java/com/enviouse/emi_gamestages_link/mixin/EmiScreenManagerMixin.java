package com.enviouse.emi_gamestages_link.mixin;

import com.enviouse.emi_gamestages_link.LockRenderHelper;
import com.enviouse.emi_gamestages_link.ModConfiguration;
import com.enviouse.emi_gamestages_link.StageLockResolver;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to render lock icons AFTER all items in the search panel are drawn.
 * This ensures locks always appear on top of items.
 */
@Mixin(value = EmiScreenManager.ScreenSpace.class, remap = false)
public abstract class EmiScreenManagerMixin {

    @Shadow
    @Final
    public int pageSize;

    @Shadow
    @Final
    public int th;

    @Shadow
    @Final
    public int[] widths;

    @Shadow
    public abstract List<? extends EmiIngredient> getStacks();

    @Shadow
    public abstract int getX(int x, int y);

    @Shadow
    public abstract int getY(int x, int y);

    /**
     * Inject after the batcher.draw() call to render locks on top of all items.
     */
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void emi_gamestages_link$renderLocksAfterBatch(EmiDrawContext context, int mouseX, int mouseY, float delta, int startIndex, CallbackInfo ci) {
        if (!ModConfiguration.showLockOnItemList || !ModConfiguration.showLockIcon) {
            return;
        }

        if (pageSize <= 0) {
            return;
        }

        try {
            GuiGraphics graphics = context.raw();
            List<? extends EmiIngredient> stacks = getStacks();

            int i = startIndex;
            for (int yo = 0; yo < th; yo++) {
                for (int xo = 0; xo < widths[yo]; xo++) {
                    if (i >= stacks.size()) {
                        return;
                    }
                    int cx = getX(xo, yo);
                    int cy = getY(xo, yo);
                    EmiIngredient stack = stacks.get(i++);

                    // Check if this item is locked
                    for (EmiStack emiStack : stack.getEmiStacks()) {
                        ItemStack itemStack = emiStack.getItemStack();
                        if (itemStack.isEmpty()) {
                            continue;
                        }

                        StageLockResolver.LockInfo lockInfo = StageLockResolver.getLockInfo(itemStack);
                        if (lockInfo != null && lockInfo.isLocked()) {
                            // Draw lock at top-left of slot (slot position is cx+1, cy+1)
                            int padding = ModConfiguration.iconPadding;
                            int iconX = cx + 1 + padding;
                            int iconY = cy + 1 + padding;
                            LockRenderHelper.drawLockIconAt(graphics, iconX, iconY);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail if something goes wrong
        }
    }
}
