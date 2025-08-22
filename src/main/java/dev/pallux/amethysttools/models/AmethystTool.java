package dev.pallux.amethysttools.models;

import java.util.UUID;

public class AmethystTool {

    private final UUID toolUUID;
    private final ToolType toolType;
    private final long creationTime;
    private UUID ownerUUID;

    public AmethystTool(UUID toolUUID, ToolType toolType, long creationTime, UUID ownerUUID) {
        this.toolUUID = toolUUID;
        this.toolType = toolType;
        this.creationTime = creationTime;
        this.ownerUUID = ownerUUID;
    }

    public UUID getToolUUID() {
        return toolUUID;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AmethystTool that = (AmethystTool) obj;
        return toolUUID.equals(that.toolUUID);
    }

    @Override
    public int hashCode() {
        return toolUUID.hashCode();
    }

    @Override
    public String toString() {
        return "AmethystTool{" +
                "toolUUID=" + toolUUID +
                ", toolType=" + toolType +
                ", creationTime=" + creationTime +
                ", ownerUUID=" + ownerUUID +
                '}';
    }
}