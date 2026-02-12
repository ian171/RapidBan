package net.chen.rapidBan.models;

public class IPRecord {
    private long id;
    private String uuid;
    private String username;
    private String ipAddress;
    private long firstSeen;
    private long lastSeen;
    private int loginCount;

    public IPRecord() {}

    public IPRecord(String uuid, String username, String ipAddress, long timestamp) {
        this.uuid = uuid;
        this.username = username;
        this.ipAddress = ipAddress;
        this.firstSeen = timestamp;
        this.lastSeen = timestamp;
        this.loginCount = 1;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(long firstSeen) { this.firstSeen = firstSeen; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    public void incrementLoginCount() {
        this.loginCount++;
    }
}
