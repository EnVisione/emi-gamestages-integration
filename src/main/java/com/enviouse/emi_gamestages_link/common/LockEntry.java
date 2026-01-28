package com.enviouse.emi_gamestages_link.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a single lock entry - an item or recipe locked to a stage.
 * This is a data transfer object used for network sync.
 */
public record LockEntry(
        ResourceLocation targetId,
        EntryType entryType,
        String requiredStage,
        LockType source,
        @Nullable String reason
) {

    /**
     * Type of entry (item or recipe)
     */
    public enum EntryType {
        ITEM,
        RECIPE
    }

    /**
     * Write this entry to a network buffer
     */
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(targetId);
        buf.writeEnum(entryType);
        buf.writeUtf(requiredStage);
        buf.writeEnum(source);
        buf.writeBoolean(reason != null);
        if (reason != null) {
            buf.writeUtf(reason);
        }
    }

    /**
     * Read an entry from a network buffer
     */
    public static LockEntry fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation targetId = buf.readResourceLocation();
        EntryType entryType = buf.readEnum(EntryType.class);
        String requiredStage = buf.readUtf();
        LockType source = buf.readEnum(LockType.class);
        String reason = buf.readBoolean() ? buf.readUtf() : null;
        return new LockEntry(targetId, entryType, requiredStage, source, reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LockEntry lockEntry = (LockEntry) o;
        return Objects.equals(targetId, lockEntry.targetId) &&
               entryType == lockEntry.entryType &&
               Objects.equals(requiredStage, lockEntry.requiredStage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetId, entryType, requiredStage);
    }
}
