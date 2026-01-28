package com.enviouse.emi_gamestages_link.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A snapshot of all lock data for a player.
 * Used for full synchronization from server to client.
 */
public class LockSnapshot {

    private final Map<ResourceLocation, LockEntry> itemLocks;
    private final Map<ResourceLocation, LockEntry> recipeLocks;
    private final Set<String> unlockedStages;

    public LockSnapshot() {
        this.itemLocks = new HashMap<>();
        this.recipeLocks = new HashMap<>();
        this.unlockedStages = new HashSet<>();
    }

    public LockSnapshot(Map<ResourceLocation, LockEntry> itemLocks,
                        Map<ResourceLocation, LockEntry> recipeLocks,
                        Set<String> unlockedStages) {
        this.itemLocks = new HashMap<>(itemLocks);
        this.recipeLocks = new HashMap<>(recipeLocks);
        this.unlockedStages = new HashSet<>(unlockedStages);
    }

    public void addItemLock(LockEntry entry) {
        itemLocks.put(entry.targetId(), entry);
    }

    public void addRecipeLock(LockEntry entry) {
        recipeLocks.put(entry.targetId(), entry);
    }

    public void addUnlockedStage(String stage) {
        unlockedStages.add(stage);
    }

    public void setUnlockedStages(Collection<String> stages) {
        unlockedStages.clear();
        unlockedStages.addAll(stages);
    }

    @Nullable
    public LockEntry getItemLock(ResourceLocation itemId) {
        return itemLocks.get(itemId);
    }

    @Nullable
    public LockEntry getRecipeLock(ResourceLocation recipeId) {
        return recipeLocks.get(recipeId);
    }

    public boolean hasStage(String stage) {
        return unlockedStages.contains(stage);
    }

    public Set<String> getUnlockedStages() {
        return Collections.unmodifiableSet(unlockedStages);
    }

    public Map<ResourceLocation, LockEntry> getItemLocks() {
        return Collections.unmodifiableMap(itemLocks);
    }

    public Map<ResourceLocation, LockEntry> getRecipeLocks() {
        return Collections.unmodifiableMap(recipeLocks);
    }

    /**
     * Check if an item is locked (requires a stage the player doesn't have)
     */
    public boolean isItemLocked(ResourceLocation itemId) {
        LockEntry entry = itemLocks.get(itemId);
        if (entry == null) {
            return false;
        }
        return !unlockedStages.contains(entry.requiredStage());
    }

    /**
     * Check if a recipe is locked (requires a stage the player doesn't have)
     */
    public boolean isRecipeLocked(ResourceLocation recipeId) {
        LockEntry entry = recipeLocks.get(recipeId);
        if (entry == null) {
            return false;
        }
        return !unlockedStages.contains(entry.requiredStage());
    }

    /**
     * Merge another snapshot into this one (for delta updates)
     */
    public void merge(LockSnapshot other) {
        this.itemLocks.putAll(other.itemLocks);
        this.recipeLocks.putAll(other.recipeLocks);
        this.unlockedStages.clear();
        this.unlockedStages.addAll(other.unlockedStages);
    }

    /**
     * Write this snapshot to a network buffer
     */
    public void toNetwork(FriendlyByteBuf buf) {
        // Write item locks
        buf.writeVarInt(itemLocks.size());
        for (LockEntry entry : itemLocks.values()) {
            entry.toNetwork(buf);
        }

        // Write recipe locks
        buf.writeVarInt(recipeLocks.size());
        for (LockEntry entry : recipeLocks.values()) {
            entry.toNetwork(buf);
        }

        // Write unlocked stages
        buf.writeVarInt(unlockedStages.size());
        for (String stage : unlockedStages) {
            buf.writeUtf(stage);
        }
    }

    /**
     * Read a snapshot from a network buffer
     */
    public static LockSnapshot fromNetwork(FriendlyByteBuf buf) {
        LockSnapshot snapshot = new LockSnapshot();

        // Read item locks
        int itemCount = buf.readVarInt();
        for (int i = 0; i < itemCount; i++) {
            LockEntry entry = LockEntry.fromNetwork(buf);
            snapshot.itemLocks.put(entry.targetId(), entry);
        }

        // Read recipe locks
        int recipeCount = buf.readVarInt();
        for (int i = 0; i < recipeCount; i++) {
            LockEntry entry = LockEntry.fromNetwork(buf);
            snapshot.recipeLocks.put(entry.targetId(), entry);
        }

        // Read unlocked stages
        int stageCount = buf.readVarInt();
        for (int i = 0; i < stageCount; i++) {
            snapshot.unlockedStages.add(buf.readUtf());
        }

        return snapshot;
    }

    public int getTotalLockCount() {
        return itemLocks.size() + recipeLocks.size();
    }
}
