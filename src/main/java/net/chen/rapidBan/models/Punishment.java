package net.chen.rapidBan.models;

import lombok.*;
import net.chen.rapidBan.enums.PunishmentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Punishment {
    private long id;
    private String uuid;
    private PunishmentType type;
    private String reason;
    private String operator;
    private String operatorName;
    private long createdAt;
    private Long expiresAt;
    private boolean active;
    private boolean revoked;
    private String revokedBy;
    private Long revokedAt;
    private String revokeReason;
    private boolean silent;
    private String serverId;

    public Punishment(String uuid, PunishmentType type, String reason, String operator, String operatorName, long createdAt) {
        this.uuid = uuid;
        this.type = type;
        this.reason = reason;
        this.operator = operator;
        this.operatorName = operatorName;
        this.createdAt = createdAt;
        this.active = true;
        this.revoked = false;
        this.silent = false;
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTime() {
        if (expiresAt == null) return -1;
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
