package com.enviouse.emi_gamestages_link.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for EMI GameStages Link
 */
@Mod.EventBusSubscriber(modid = EmiGameStagesLink.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfiguration {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Display settings
    private static final ForgeConfigSpec.BooleanValue SHOW_LOCK_ICON;
    private static final ForgeConfigSpec.BooleanValue HIGHLIGHT_LOCKED_OUTPUT;
    private static final ForgeConfigSpec.IntValue ICON_SIZE;
    private static final ForgeConfigSpec.IntValue ICON_PADDING;
    private static final ForgeConfigSpec.BooleanValue SHOW_LOCK_ON_ITEM_LIST;
    private static final ForgeConfigSpec.IntValue HIGHLIGHT_COLOR;
    private static final ForgeConfigSpec.BooleanValue SHOW_TOOLTIP_INFO;
    private static final ForgeConfigSpec.BooleanValue SHOW_LOCKED_RECIPES;

    // Integration settings
    private static final ForgeConfigSpec.BooleanValue ENABLE_ITEMSTAGES_INTEGRATION;
    private static final ForgeConfigSpec.BooleanValue ENABLE_RECIPESTAGES_INTEGRATION;

    // Lock definitions
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_LOCKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TAG_LOCKS;

    static {
        BUILDER.comment("Display Settings").push("display");

        SHOW_LOCK_ICON = BUILDER
                .comment("Show lock icon on locked items/outputs")
                .define("showLockIcon", true);

        HIGHLIGHT_LOCKED_OUTPUT = BUILDER
                .comment("Highlight locked recipe outputs with a colored overlay")
                .define("highlightLockedOutput", true);

        ICON_SIZE = BUILDER
                .comment("Size of the lock icon in pixels (6-16)")
                .defineInRange("iconSize", 8, 6, 16);

        ICON_PADDING = BUILDER
                .comment("Padding from the edge of the slot for the lock icon")
                .defineInRange("iconPadding", 1, 0, 4);

        SHOW_LOCK_ON_ITEM_LIST = BUILDER
                .comment("Show lock icon on items in EMI search/item list panels")
                .define("showLockOnItemList", true);

        HIGHLIGHT_COLOR = BUILDER
                .comment("Color for the locked output highlight (ARGB hex, e.g., 0x50FFAA40 for semi-transparent light orange)")
                .defineInRange("highlightColor", 0x50FFAA40, Integer.MIN_VALUE, Integer.MAX_VALUE);

        SHOW_TOOLTIP_INFO = BUILDER
                .comment("Show locked stage information in item tooltips when hovering")
                .define("showTooltipInfo", true);

        // New flag: prevent JEI from hiding locked recipes/ingredients when true
        SHOW_LOCKED_RECIPES = BUILDER
                .comment("If true, attempt to prevent JEI (and JEI-based plugins) from hiding locked recipes/ingredients; EMI will re-add missing ingredients on stage sync.")
                .define("showLockedRecipes", true);

        BUILDER.pop();

        BUILDER.comment("Integration Settings - Automatic detection from other mods").push("integration");

        ENABLE_ITEMSTAGES_INTEGRATION = BUILDER
                .comment("Enable automatic integration with ItemStages mod (detects item restrictions automatically)")
                .define("enableItemStages", true);

        ENABLE_RECIPESTAGES_INTEGRATION = BUILDER
                .comment("Enable automatic integration with RecipeStages mod (detects recipe restrictions automatically)")
                .define("enableRecipeStages", true);

        BUILDER.pop();

        BUILDER.comment("Manual Item Locks - Format: 'item_id=stage_name' (e.g., 'minecraft:diamond_pickaxe=diamond_age')",
                        "These are checked AFTER automatic integrations, useful for custom locks").push("locks");

        ITEM_LOCKS = BUILDER
                .comment("List of item locks in format 'modid:item_id=stage_name'")
                .defineListAllowEmpty("items", List.of(), ModConfiguration::validateLockEntry);

        TAG_LOCKS = BUILDER
                .comment("List of tag locks in format '#modid:tag=stage_name' (e.g., '#forge:ingots/steel=steel_age')")
                .defineListAllowEmpty("tags", List.of(), ModConfiguration::validateLockEntry);

        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime cached values
    public static boolean showLockIcon;
    public static boolean highlightLockedOutput;
    public static int iconSize;
    public static int iconPadding;
    public static boolean showLockOnItemList;
    public static int highlightColor;
    public static boolean showTooltipInfo;
    public static boolean showLockedRecipes;
    public static boolean enableItemStagesIntegration;
    public static boolean enableRecipeStagesIntegration;
    public static Map<String, String> itemLocks = new HashMap<>();
    public static Map<String, String> tagLocks = new HashMap<>();

    private static boolean validateLockEntry(final Object obj) {
        if (!(obj instanceof String str)) return false;
        return str.contains("=") && str.split("=").length == 2;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        showLockIcon = SHOW_LOCK_ICON.get();
        highlightLockedOutput = HIGHLIGHT_LOCKED_OUTPUT.get();
        iconSize = ICON_SIZE.get();
        iconPadding = ICON_PADDING.get();
        showLockOnItemList = SHOW_LOCK_ON_ITEM_LIST.get();
        highlightColor = HIGHLIGHT_COLOR.get();
        showTooltipInfo = SHOW_TOOLTIP_INFO.get();
        showLockedRecipes = SHOW_LOCKED_RECIPES.get();
        enableItemStagesIntegration = ENABLE_ITEMSTAGES_INTEGRATION.get();
        enableRecipeStagesIntegration = ENABLE_RECIPESTAGES_INTEGRATION.get();

        // Parse item locks
        itemLocks.clear();
        for (String entry : ITEM_LOCKS.get()) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                itemLocks.put(parts[0].trim(), parts[1].trim());
            }
        }

        // Parse tag locks
        tagLocks.clear();
        for (String entry : TAG_LOCKS.get()) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                tagLocks.put(parts[0].trim(), parts[1].trim());
            }
        }

        EmiGameStagesLink.LOGGER.info("EMI GameStages Link config loaded: {} item locks, {} tag locks",
                itemLocks.size(), tagLocks.size());
    }
}
