package net.chen.rapidBan.database;

import lombok.Getter;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.models.IPRecord;
import org.jspecify.annotations.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IPRepository {
    @Getter
    private final RapidBan plugin;
    private final DatabaseManager database;
    private final boolean isSQLite;

    public IPRepository(RapidBan plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.isSQLite = plugin.getConfig().getString("database.type", "MYSQL").equalsIgnoreCase("SQLITE");
    }

    public CompletableFuture<Void> recordIPLogin(IPRecord record) {
        return database.executeAsyncVoid(conn -> {
            String sql = getString();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, record.getUuid());
                stmt.setString(2, record.getUsername());
                stmt.setString(3, record.getIpAddress());
                stmt.setLong(4, record.getFirstSeen());
                stmt.setLong(5, record.getLastSeen());
                stmt.setInt(6, record.getLoginCount());

                if (!isSQLite) {
                    stmt.setString(7, record.getUsername());
                    stmt.setLong(8, record.getLastSeen());
                }

                stmt.executeUpdate();
            }
        });
    }

    private @NonNull String getString() {
        String sql;
        if (isSQLite) {
            sql = "INSERT INTO rb_ip_history (uuid, username, ip_address, first_seen, last_seen, login_count) VALUES (?, ?, ?, ?, ?, ?) " +
                  "ON CONFLICT(uuid, ip_address) DO UPDATE SET username = excluded.username, last_seen = excluded.last_seen, login_count = login_count + 1";
        } else {
            sql = "INSERT INTO rb_ip_history (uuid, username, ip_address, first_seen, last_seen, login_count) VALUES (?, ?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE username = ?, last_seen = ?, login_count = login_count + 1";
        }
        return sql;
    }

    public CompletableFuture<List<IPRecord>> getIPsByUUID(String uuid) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_ip_history WHERE uuid = ? ORDER BY last_seen DESC";
            List<IPRecord> records = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapIPRecord(rs));
                    }
                }
            }
            return records;
        });
    }

    public CompletableFuture<List<IPRecord>> getUUIDsByIP(String ipAddress) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_ip_history WHERE ip_address = ? ORDER BY last_seen DESC";
            List<IPRecord> records = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ipAddress);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapIPRecord(rs));
                    }
                }
            }
            return records;
        });
    }

    private IPRecord mapIPRecord(ResultSet rs) throws SQLException {
        IPRecord record = new IPRecord();
        record.setId(rs.getLong("id"));
        record.setUuid(rs.getString("uuid"));
        record.setUsername(rs.getString("username"));
        record.setIpAddress(rs.getString("ip_address"));
        record.setFirstSeen(rs.getLong("first_seen"));
        record.setLastSeen(rs.getLong("last_seen"));
        record.setLoginCount(rs.getInt("login_count"));
        return record;
    }
}
