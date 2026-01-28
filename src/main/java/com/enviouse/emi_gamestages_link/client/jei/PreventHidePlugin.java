package com.enviouse.emi_gamestages_link.client.jei;

import com.enviouse.emi_gamestages_link.common.EmiGameStagesLink;
import com.enviouse.emi_gamestages_link.common.ModConfiguration;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.helpers.IJeiHelpers;
import net.darkhax.gamestages.event.StagesSyncedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@JeiPlugin
public class PreventHidePlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(EmiGameStagesLink.MODID, "prevent_hide");

    private IJeiRuntime runtime;

    public PreventHidePlugin() {
        // Listen for stage syncs and recipe updates — schedule re-add attempts to run on client thread
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, StagesSyncedEvent.class, e -> scheduleReadd());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RecipesUpdatedEvent.class, e -> scheduleReadd());
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
        EmiGameStagesLink.LOGGER.info("PreventHidePlugin: JEI runtime available");
        // Register a listener so we can immediately re-add any ItemStack ingredients removed by other plugins
        try {
            IIngredientManager mgr = this.runtime.getIngredientManager();
            mgr.registerIngredientListener(new IIngredientManager.IIngredientListener() {
                @Override
                public <V> void onIngredientsAdded(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
                    // no-op
                }

                @Override
                public <V> void onIngredientsRemoved(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
                    if (!ModConfiguration.showLockedRecipes) return;
                    try {
                        List<ItemStack> toReadd = new ArrayList<>();
                        for (ITypedIngredient<V> typed : ingredients) {
                            try {
                                Object raw = typed.getIngredient();
                                if (raw instanceof ItemStack is) {
                                    toReadd.add(is);
                                }
                            } catch (Throwable ignored) {}
                        }
                        if (!toReadd.isEmpty()) {
                            // Re-add immediately on client thread
                            Minecraft mc = Minecraft.getInstance();
                            if (mc != null) {
                                mc.execute(() -> {
                                    try {
                                        mgr.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toReadd);
                                        EmiGameStagesLink.LOGGER.info("PreventHidePlugin: re-added {} stacks from removal listener", toReadd.size());
                                        refreshIngredientVisibility(mgr, toReadd);
                                    } catch (Throwable t) { EmiGameStagesLink.LOGGER.debug("PreventHidePlugin readd in listener failed: {}", t.getMessage()); }
                                });
                            } else {
                                try { mgr.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toReadd); EmiGameStagesLink.LOGGER.info("PreventHidePlugin: re-added {} stacks from removal listener", toReadd.size()); refreshIngredientVisibility(mgr, toReadd); } catch (Throwable t) { EmiGameStagesLink.LOGGER.debug("PreventHidePlugin readd in listener failed: {}", t.getMessage()); }
                            }
                        }
                    } catch (Throwable t) {
                        EmiGameStagesLink.LOGGER.debug("PreventHidePlugin listener failed: {}", t.getMessage());
                    }
                }
            });
            EmiGameStagesLink.LOGGER.info("PreventHidePlugin: registered JEI ingredient listener");
        } catch (Throwable t) {
            EmiGameStagesLink.LOGGER.debug("PreventHidePlugin failed to register ingredient listener: {}", t.getMessage());
        }

        scheduleReadd();
    }

    private void scheduleReadd() {
        if (!ModConfiguration.showLockedRecipes) return;
        Minecraft mc = Minecraft.getInstance();
        // Run a repeated re-add for a few seconds to outrun other plugins that may hide later
        final int totalMs = 6000;
        final int intervalMs = 250;
        Thread t = new Thread(() -> {
            int iterations = Math.max(1, totalMs / intervalMs);
            for (int i = 0; i < iterations; i++) {
                try {
                    if (mc != null) {
                        mc.execute(() -> {
                            try { readdAllIfEnabled(); } catch (Throwable t2) { EmiGameStagesLink.LOGGER.debug("PreventHidePlugin readd failed: {}", t2.getMessage()); }
                        });
                    } else {
                        try { readdAllIfEnabled(); } catch (Throwable t2) { EmiGameStagesLink.LOGGER.debug("PreventHidePlugin readd failed: {}", t2.getMessage()); }
                    }
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ignored) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "PreventHidePlugin-ReaddLoop");
        t.setDaemon(true);
        t.start();
    }

    private void readdAllIfEnabled() {
        if (!ModConfiguration.showLockedRecipes) return;
        if (this.runtime == null) return;

        try {
            IIngredientManager ingredients = this.runtime.getIngredientManager();

            // Gather existing JEI item registry ids to avoid duplicates
            Set<String> present = new HashSet<>();
            Collection<ItemStack> current = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK);
            for (ItemStack s : current) {
                if (s == null || s.isEmpty()) continue;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
                if (id != null) present.add(id.toString());
            }

            // Build list of items that are not present in JEI and add them
            List<ItemStack> toAdd = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                if (item == null) continue;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id == null) continue;
                if (!present.contains(id.toString())) {
                    ItemStack s = new ItemStack(item);
                    if (!s.isEmpty()) toAdd.add(s);
                }
            }

            if (!toAdd.isEmpty()) {
                ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toAdd);
                EmiGameStagesLink.LOGGER.debug("PreventHidePlugin restored {} item stacks into JEI", toAdd.size());
                // Force JEI visibility refresh for the stacks we just added
                refreshIngredientVisibility(ingredients, toAdd);
            }
        } catch (Throwable t) {
            EmiGameStagesLink.LOGGER.debug("PreventHidePlugin failed to restore JEI ingredients: {}", t.getMessage());
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private void refreshIngredientVisibility(IIngredientManager ingredients, List<ItemStack> items) {
        if (this.runtime == null) return;
        try {
            IJeiHelpers helpers = this.runtime.getJeiHelpers();
            Object visibility = helpers.getIngredientVisibility();
            if (visibility == null) return;

            Method notify = null;
            try {
                notify = visibility.getClass().getMethod("notifyListeners", mezz.jei.api.ingredients.ITypedIngredient.class, boolean.class);
            } catch (NoSuchMethodException e) {
                // Older JEI might use different signature - try to find compatible method
                for (Method m : visibility.getClass().getMethods()) {
                    if (m.getName().equals("notifyListeners") && m.getParameterCount() == 2) {
                        notify = m;
                        break;
                    }
                }
            }

            if (notify == null) return;

            for (ItemStack s : items) {
                try {
                    Optional typed = ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, s);
                    if (typed.isPresent()) {
                        Object t = typed.get();
                        // invoke notifyListeners(typedIngredient, true)
                        notify.invoke(visibility, t, true);
                    }
                } catch (Throwable t) {
                    // ignore individual failures
                }
            }
            EmiGameStagesLink.LOGGER.info("PreventHidePlugin: requested JEI visibility refresh for {} items", items.size());
        } catch (Throwable t) {
            EmiGameStagesLink.LOGGER.debug("PreventHidePlugin: visibility refresh failed: {}", t.getMessage());
        }
    }
}
