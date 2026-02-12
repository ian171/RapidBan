package net.chen.rapidBan.database;

import lombok.Getter;
import net.chen.rapidBan.RapidBan;
import net.chen.rapidBan.enums.PunishmentType;
import net.chen.rapidBan.models.Punishment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PunishmentRepository {
    @Getter
    private final RapidBan plugin;
    private final DatabaseManager database;

    public PunishmentRepository(RapidBan plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public CompletableFuture<Long> createPunishment(Punishment punishment) {
        return database.executeAsync(conn -> {
            String sql = "INSERT INTO rb_punishments (uuid, type, reason, operator, operator_name, created_at, expires_at, active, silent, server_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, punishment.getUuid());
                stmt.setString(2, punishment.getType().name());
                stmt.setString(3, punishment.getReason());
                stmt.setString(4, punishment.getOperator());
                stmt.setString(5, punishment.getOperatorName());
                stmt.setLong(6, punishment.getCreatedAt());
                if (punishment.getExpiresAt() != null) {
                    stmt.setLong(7, punishment.getExpiresAt());
                } else {
                    stmt.setNull(7, Types.BIGINT);
                }
                stmt.setBoolean(8, punishment.isActive());
                stmt.setBoolean(9, punishment.isSilent());
                stmt.setString(10, punishment.getServerId());

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

    public CompletableFuture<Optional<Punishment>> getActiveBan(String uuid) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_punishments WHERE uuid = ? AND type IN ('BAN', 'TEMPBAN') AND active = TRUE AND revoked = FALSE ORDER BY created_at DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapPunishment(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(String uuid) {
        return database.executeAsync(conn -> {
            String sql = "SELECT * FROM rb_punishments WHERE uuid = ? ORDER BY created_at DESC";
            List<Punishment> punishments = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapPunishment(rs));
                    }
                }
            }
            return punishments;
        });
    }

    public CompletableFuture<Void> deactivatePunishment(long id) {
        return database.executeAsyncVoid(conn -> {
            String sql = "UPDATE rb_punishments SET active = FALSE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> revokePunishment(long id, String revokedBy, String reason) {
        return database.executeAsyncVoid(conn -> {
            String sql = "UPDATE rb_punishments SET revoked = TRUE, revoked_by = ?, revoked_at = ?, revoke_reason = ?, active = FALSE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, revokedBy);
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, reason);
                stmt.setLong(4, id);
                stmt.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> revokeAllPunishments(String uuid, String revokedBy, String reason) {
        return database.executeAsyncVoid(conn -> {
            String sql = "UPDATE rb_punishments SET revoked = TRUE, revoked_by = ?, revoked_at = ?, revoke_reason = ?, active = FALSE WHERE uuid = ? AND active = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, revokedBy);
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, reason);
                stmt.setString(4, uuid);
                stmt.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> checkExpiredPunishments() {
        return database.executeAsyncVoid(conn -> {
            String sql = "UPDATE rb_punishments SET active = FALSE WHERE active = TRUE AND expires_at IS NOT NULL AND expires_at < ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        });
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(rs.getLong("id"));
        punishment.setUuid(rs.getString("uuid"));
        punishment.setType(PunishmentType.valueOf(rs.getString("type")));
        punishment.setReason(rs.getString("reason"));
        punishment.setOperator(rs.getString("operator"));
        punishment.setOperatorName(rs.getString("operator_name"));
        punishment.setCreatedAt(rs.getLong("created_at"));

        long expiresAt = rs.getLong("expires_at");
        punishment.setExpiresAt(rs.wasNull() ? null : expiresAt);

        punishment.setActive(rs.getBoolean("active"));
        punishment.setRevoked(rs.getBoolean("revoked"));
        punishment.setRevokedBy(rs.getString("revoked_by"));

        long revokedAt = rs.getLong("revoked_at");
        punishment.setRevokedAt(rs.wasNull() ? null : revokedAt);

        punishment.setRevokeReason(rs.getString("revoke_reason"));
        punishment.setSilent(rs.getBoolean("silent"));
        punishment.setServerId(rs.getString("server_id"));

        return punishment;
    }
}
