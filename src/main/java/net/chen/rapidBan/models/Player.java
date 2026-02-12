package net.chen.rapidBan.models;

public class Player {
    private long id;
    private String uuid;
    private String username;
    private long firstJoin;
    private long lastJoin;
    private String lastIp;

    public Player() {}

    public Player(String uuid, String username, long firstJoin, String lastIp) {
        this.uuid = uuid;
        this.username = username;
        this.firstJoin = firstJoin;
        this.lastJoin = firstJoin;
        this.lastIp = lastIp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getFirstJoin() { return firstJoin; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }

    public long getLastJoin() { return lastJoin; }
    public void setLastJoin(long lastJoin) { this.lastJoin = lastJoin; }

    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
}
