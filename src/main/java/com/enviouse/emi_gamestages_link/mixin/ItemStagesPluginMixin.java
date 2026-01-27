package com.enviouse.emi_gamestages_link.mixin;

import com.enviouse.emi_gamestages_link.ModConfiguration;
import mezz.jei.api.runtime.IIngredientManager;
import net.darkhax.itemstages.jei.PluginItemStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent ItemStages JEI plugin from hiding ingredients when our config requests it.
 */
@Mixin(value = PluginItemStages.class, remap = false)
public class ItemStagesPluginMixin {

    @Inject(method = "hideStagedIngredients", at = @At("HEAD"), cancellable = true, remap = false)
    private void emi_gamestages_link$onHideStagedIngredients(IIngredientManager ingredients, CallbackInfo ci) {
        if (ModConfiguration.showLockedRecipes) {
            // If configured to show locked recipes, cancel ItemStages' hiding logic
            ci.cancel();
        }
    }
}
