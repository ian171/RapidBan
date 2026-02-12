package net.chen.rapidBan.models;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IPRecord {
    private long id;
    private String uuid;
    private String username;
    private String ipAddress;
    private long firstSeen;
    private long lastSeen;
    private int loginCount;

    public IPRecord(String uuid, String username, String ipAddress, long timestamp) {
        this.uuid = uuid;
        this.username = username;
        this.ipAddress = ipAddress;
        this.firstSeen = timestamp;
        this.lastSeen = timestamp;
        this.loginCount = 1;
    }

    public void incrementLoginCount() {
        this.loginCount++;
    }
}
