package net.chen.rapidBan.database;

import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.Player;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {
    private final RapidBan plugin;
    private final DatabaseManager database;
    private final boolean isSQLite;

    public PlayerRepository(RapidBan plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.isSQLite = plugin.getConfig().getString("database.type", "MYSQL").equalsIgnoreCase("SQLITE");
    }

    public CompletableFuture<Void> createOrUpdatePlayer(Player player) {
        return database.executeAsyncVoid(conn -> {
            String sql;
            if (isSQLite) {
                sql = "INSERT INTO rb_players (uuid, username, first_join, last_join, last_ip) VALUES (?, ?, ?, ?, ?) " +
                      "ON CONFLICT(uuid) DO UPDATE SET username = excluded.username, last_join = excluded.last_join, last_ip = excluded.last_ip";
            } else {
                sql = "INSERT INTO rb_players (uuid, username, first_join, last_join, last_ip) VALUES (?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE username = ?, last_join = ?, last_ip = ?";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUuid());
                stmt.setString(2, player.getUsername());
                stmt.setLong(3, player.getFirstJoin());
                stmt.setLong(4, player.getLastJoin());
                stmt.setString(5, player.getLastIp());

                if (!isSQLite) {
                    stmt.setString(6, player.getUsername());
                    stmt.setLong(7, player.getLastJoin());
                    stmt.setString(8, player.getLastIp());
                }

                stmt.executeUpdate();
            }
        });
    }

    public CompletableFuture<Optional<Player>> getPlayer(String uuid) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_players WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapPlayer(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<Player>> getPlayerByName(String username) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_players WHERE username = ? ORDER BY last_join DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapPlayer(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    private Player mapPlayer(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.setId(rs.getLong("id"));
        player.setUuid(rs.getString("uuid"));
        player.setUsername(rs.getString("username"));
        player.setFirstJoin(rs.getLong("first_join"));
        player.setLastJoin(rs.getLong("last_join"));
        player.setLastIp(rs.getString("last_ip"));
        return player;
    }
}
