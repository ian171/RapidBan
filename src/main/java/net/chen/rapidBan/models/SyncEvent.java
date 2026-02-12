package net.chen.rapidBan.models;

import lombok.*;
import net.chen.rapidBan.enums.SyncEventType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncEvent {
    private long id;
    private SyncEventType eventType;
    private String targetUuid;
    private Long punishmentId;
    private String data;
    private String serverId;
    private long createdAt;
    private boolean processed;

    public SyncEvent(SyncEventType eventType, String targetUuid, String serverId) {
        this.eventType = eventType;
        this.targetUuid = targetUuid;
        this.serverId = serverId;
        this.createdAt = System.currentTimeMillis();
        this.processed = false;
    }
}
