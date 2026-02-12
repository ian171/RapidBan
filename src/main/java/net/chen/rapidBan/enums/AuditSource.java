package net.chen.rapidBan.enums;

public enum AuditSource {
    GAME("游戏内"),
    WEB("Web面板"),
    API("API接口");

    private final String displayName;

    AuditSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
