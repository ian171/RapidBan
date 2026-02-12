package net.chen.rapidBan.models;

import net.chen.rapidBan.enums.SyncEventType;

public class SyncEvent {
    private long id;
    private SyncEventType eventType;
    private String targetUuid;
    private Long punishmentId;
    private String data;
    private String serverId;
    private long createdAt;
    private boolean processed;

    public SyncEvent() {}

    public SyncEvent(SyncEventType eventType, String targetUuid, String serverId) {
        this.eventType = eventType;
        this.targetUuid = targetUuid;
        this.serverId = serverId;
        this.createdAt = System.currentTimeMillis();
        this.processed = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public SyncEventType getEventType() { return eventType; }
    public void setEventType(SyncEventType eventType) { this.eventType = eventType; }

    public String getTargetUuid() { return targetUuid; }
    public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

    public Long getPunishmentId() { return punishmentId; }
    public void setPunishmentId(Long punishmentId) { this.punishmentId = punishmentId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
