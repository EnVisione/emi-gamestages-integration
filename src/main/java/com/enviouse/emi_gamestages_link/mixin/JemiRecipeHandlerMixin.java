package com.enviouse.emi_gamestages_link.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.emi.jemi.JemiRecipeHandler;
import dev.emi.emi.runtime.EmiLog;

/**
 * Redirect the unsafe List.subList(...) call inside
 * JemiRecipeHandler.createSlotsView(...) to a safe helper.
 *
 * If an out-of-bounds range would be used, we clamp it to the
 * list size or return an empty list instead of letting EMI crash.
 */
@Mixin(value = JemiRecipeHandler.class, remap = false)
public abstract class JemiRecipeHandlerMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "createSlotsView", at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;"))
    private List emi_gamestages_link$safeSubList(List list, int fromIndex, int toIndex) {
        try {
            if (list == null) return List.of();
            int size = list.size();
            int safeFrom = Math.max(0, Math.min(size, fromIndex));
            int safeTo = Math.max(safeFrom, Math.min(size, toIndex));
            if (safeFrom == safeTo) return List.of();
            return list.subList(safeFrom, safeTo);
        } catch (IndexOutOfBoundsException e) {
            EmiLog.error("JemiRecipeHandler: caught IndexOutOfBoundsException when creating subList - returning empty list to avoid crash", e);
            return List.of();
        } catch (Throwable t) {
            EmiLog.error("JemiRecipeHandler: unexpected error in safeSubList - returning empty list", t);
            return List.of();
        }
    }
}
