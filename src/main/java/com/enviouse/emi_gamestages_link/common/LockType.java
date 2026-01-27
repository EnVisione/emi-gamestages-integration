package com.enviouse.emi_gamestages_link.common;

/**
 * Represents the source of a lock (which mod/system defined it).
 */
public enum LockType {
    /**
     * Lock from ItemStages mod
     */
    ITEM_STAGES,

    /**
     * Lock from RecipeStages mod
     */
    RECIPE_STAGES,

    /**
     * Lock from manual config
     */
    CONFIG
}
