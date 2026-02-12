package net.chen.rapidBan.enums;

public enum PunishmentType {
    BAN("永久封禁"),
    TEMPBAN("临时封禁"),
    MUTE("永久禁言"),
    TEMPMUTE("临时禁言"),
    KICK("踢出"),
    WARN("警告");

    private final String displayName;

    PunishmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBan() {
        return this == BAN || this == TEMPBAN;
    }

    public boolean isMute() {
        return this == MUTE || this == TEMPMUTE;
    }

    public boolean isTemporary() {
        return this == TEMPBAN || this == TEMPMUTE;
    }
}
