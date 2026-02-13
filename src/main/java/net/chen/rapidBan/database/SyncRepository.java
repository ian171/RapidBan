package net.chen.rapidBan.database;

import lombok.Getter;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.enums.SyncEventType;
import net.chen.rapidBan.models.SyncEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SyncRepository {
    @Getter
    private final RapidBan plugin;
    private final DatabaseManager database;

    public SyncRepository(RapidBan plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public CompletableFuture<Long> createSyncEvent(SyncEvent event) {
        return database.executeAsync(conn -> {
            String sql = "INSERT INTO rb_sync_events (event_type, target_uuid, punishment_id, data, server_id, created_at, processed) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, event.getEventType().name());
                stmt.setString(2, event.getTargetUuid());
                if (event.getPunishmentId() != null) {
                    stmt.setLong(3, event.getPunishmentId());
                } else {
                    stmt.setNull(3, Types.BIGINT);
                }
                stmt.setString(4, event.getData());
                stmt.setString(5, event.getServerId());
                stmt.setLong(6, event.getCreatedAt());
                stmt.setBoolean(7, event.isProcessed());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            return -1L;
        });
    }

    public CompletableFuture<List<SyncEvent>> getUnprocessedEvents(String currentServerId) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_sync_events WHERE processed = FALSE AND server_id != ? ORDER BY created_at ASC LIMIT 100";
            List<SyncEvent> events = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, currentServerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        events.add(mapSyncEvent(rs));
                    }
                }
            }
            return events;
        });
    }

    public CompletableFuture<Void> markEventProcessed(long eventId) {
        return database.executeAsyncVoid(conn -> {
            String sql = "UPDATE rb_sync_events SET processed = TRUE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, eventId);
                stmt.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> cleanOldEvents(long olderThan) {
        return database.executeAsyncVoid(conn -> {
            String sql = "DELETE FROM rb_sync_events WHERE created_at < ? AND processed = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, olderThan);
                stmt.executeUpdate();
            }
        });
    }

    private SyncEvent mapSyncEvent(ResultSet rs) throws SQLException {
        SyncEvent event = new SyncEvent();
        event.setId(rs.getLong("id"));
        event.setEventType(SyncEventType.valueOf(rs.getString("event_type")));
        event.setTargetUuid(rs.getString("target_uuid"));

        long punishmentId = rs.getLong("punishment_id");
        event.setPunishmentId(rs.wasNull() ? null : punishmentId);

        event.setData(rs.getString("data"));
        event.setServerId(rs.getString("server_id"));
        event.setCreatedAt(rs.getLong("created_at"));
        event.setProcessed(rs.getBoolean("processed"));

        return event;
    }
}
