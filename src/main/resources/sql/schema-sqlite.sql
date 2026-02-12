-- RapidBan Database Schema for SQLite
-- SQLite 3.x

CREATE TABLE IF NOT EXISTS `rb_players` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `uuid` TEXT NOT NULL UNIQUE,
    `username` TEXT NOT NULL,
    `first_join` INTEGER NOT NULL,
    `last_join` INTEGER NOT NULL,
    `last_ip` TEXT
);

CREATE INDEX IF NOT EXISTS `idx_players_uuid` ON `rb_players` (`uuid`);
CREATE INDEX IF NOT EXISTS `idx_players_username` ON `rb_players` (`username`);

CREATE TABLE IF NOT EXISTS `rb_punishments` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `uuid` TEXT NOT NULL,
    `type` TEXT NOT NULL CHECK(type IN ('BAN', 'TEMPBAN', 'MUTE', 'TEMPMUTE', 'KICK', 'WARN')),
    `reason` TEXT NOT NULL,
    `operator` TEXT NOT NULL,
    `operator_name` TEXT NOT NULL,
    `created_at` INTEGER NOT NULL,
    `expires_at` INTEGER DEFAULT NULL,
    `active` INTEGER DEFAULT 1,
    `revoked` INTEGER DEFAULT 0,
    `revoked_by` TEXT DEFAULT NULL,
    `revoked_at` INTEGER DEFAULT NULL,
    `revoke_reason` TEXT DEFAULT NULL,
    `silent` INTEGER DEFAULT 0,
    `server_id` TEXT DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS `idx_punishments_uuid` ON `rb_punishments` (`uuid`);
CREATE INDEX IF NOT EXISTS `idx_punishments_active` ON `rb_punishments` (`active`);
CREATE INDEX IF NOT EXISTS `idx_punishments_type` ON `rb_punishments` (`type`);
CREATE INDEX IF NOT EXISTS `idx_punishments_created` ON `rb_punishments` (`created_at`);
CREATE INDEX IF NOT EXISTS `idx_punishments_expires` ON `rb_punishments` (`expires_at`);

CREATE TABLE IF NOT EXISTS `rb_ip_history` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `uuid` TEXT NOT NULL,
    `username` TEXT NOT NULL,
    `ip_address` TEXT NOT NULL,
    `first_seen` INTEGER NOT NULL,
    `last_seen` INTEGER NOT NULL,
    `login_count` INTEGER DEFAULT 1,
    UNIQUE(`uuid`, `ip_address`)
);

CREATE INDEX IF NOT EXISTS `idx_ip_history_uuid` ON `rb_ip_history` (`uuid`);
CREATE INDEX IF NOT EXISTS `idx_ip_history_ip` ON `rb_ip_history` (`ip_address`);
CREATE INDEX IF NOT EXISTS `idx_ip_history_last_seen` ON `rb_ip_history` (`last_seen`);

CREATE TABLE IF NOT EXISTS `rb_sync_events` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `event_type` TEXT NOT NULL CHECK(event_type IN ('BAN', 'UNBAN', 'TEMPBAN', 'MUTE', 'UNMUTE', 'REVOKE', 'UPDATE')),
    `target_uuid` TEXT NOT NULL,
    `punishment_id` INTEGER DEFAULT NULL,
    `data` TEXT,
    `server_id` TEXT NOT NULL,
    `created_at` INTEGER NOT NULL,
    `processed` INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS `idx_sync_events_processed` ON `rb_sync_events` (`processed`);
CREATE INDEX IF NOT EXISTS `idx_sync_events_created` ON `rb_sync_events` (`created_at`);
CREATE INDEX IF NOT EXISTS `idx_sync_events_target` ON `rb_sync_events` (`target_uuid`);

CREATE TABLE IF NOT EXISTS `rb_web_tokens` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `username` TEXT NOT NULL UNIQUE,
    `password_hash` TEXT NOT NULL,
    `role` TEXT NOT NULL CHECK(role IN ('ADMIN', 'MODERATOR', 'VIEWER')) DEFAULT 'VIEWER',
    `created_at` INTEGER NOT NULL,
    `last_login` INTEGER DEFAULT NULL,
    `active` INTEGER DEFAULT 1
);

CREATE INDEX IF NOT EXISTS `idx_web_tokens_username` ON `rb_web_tokens` (`username`);

CREATE TABLE IF NOT EXISTS `rb_audit_log` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `action` TEXT NOT NULL,
    `operator` TEXT NOT NULL,
    `target` TEXT,
    `details` TEXT,
    `ip_address` TEXT,
    `timestamp` INTEGER NOT NULL,
    `source` TEXT NOT NULL CHECK(source IN ('GAME', 'WEB', 'API'))
);

CREATE INDEX IF NOT EXISTS `idx_audit_log_timestamp` ON `rb_audit_log` (`timestamp`);
CREATE INDEX IF NOT EXISTS `idx_audit_log_operator` ON `rb_audit_log` (`operator`);
CREATE INDEX IF NOT EXISTS `idx_audit_log_target` ON `rb_audit_log` (`target`);
