package net.chen.rapidBan.models;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    private long id;
    private String uuid;
    private String username;
    private long firstJoin;
    private long lastJoin;
    private String lastIp;

    public Player(String uuid, String username, long firstJoin, String lastIp) {
        this.uuid = uuid;
        this.username = username;
        this.firstJoin = firstJoin;
        this.lastJoin = firstJoin;
        this.lastIp = lastIp;
    }
}
