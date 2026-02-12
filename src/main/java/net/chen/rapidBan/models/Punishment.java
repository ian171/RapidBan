package net.chen.rapidBan.models;

import net.chen.rapidBan.enums.PunishmentType;

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

    public Punishment() {}

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

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public PunishmentType getType() { return type; }
    public void setType(PunishmentType type) { this.type = type; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public Long getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Long revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }

    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

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
