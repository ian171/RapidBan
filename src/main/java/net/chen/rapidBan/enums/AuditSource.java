package net.chen.rapidBan.enums;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum AuditSource {
    GAME("游戏内"),
    WEB("Web面板"),
    API("API接口");

    private final String displayName;

    AuditSource(String displayName) {
        this.displayName = displayName;
    }

}
