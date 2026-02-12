package net.chen.rapidBan.enums;

import lombok.Getter;

@Getter
public enum WebRole {
    ADMIN("管理员", 3),
    MODERATOR("审核员", 2),
    VIEWER("查看者", 1);

    private final String displayName;
    private final int level;

    WebRole(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public boolean hasPermission(WebRole required) {
        return this.level >= required.level;
    }
}
