-- RapidBan Database Schema
-- MySQL/MariaDB 5.7+

CREATE TABLE IF NOT EXISTS `rb_players` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL UNIQUE,
    `username` VARCHAR(16) NOT NULL,
    `first_join` BIGINT NOT NULL,
    `last_join` BIGINT NOT NULL,
    `last_ip` VARCHAR(45),
    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rb_punishments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL,
    `type` ENUM('BAN', 'TEMPBAN', 'MUTE', 'TEMPMUTE', 'KICK', 'WARN') NOT NULL,
    `reason` TEXT NOT NULL,
    `operator` VARCHAR(36) NOT NULL,
    `operator_name` VARCHAR(16) NOT NULL,
    `created_at` BIGINT NOT NULL,
    `expires_at` BIGINT DEFAULT NULL,
    `active` BOOLEAN DEFAULT TRUE,
    `revoked` BOOLEAN DEFAULT FALSE,
    `revoked_by` VARCHAR(36) DEFAULT NULL,
    `revoked_at` BIGINT DEFAULT NULL,
    `revoke_reason` TEXT DEFAULT NULL,
    `silent` BOOLEAN DEFAULT FALSE,
    `server_id` VARCHAR(64) DEFAULT NULL,
    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_active` (`active`),
    INDEX `idx_type` (`type`),
    INDEX `idx_created` (`created_at`),
    INDEX `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rb_ip_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL,
    `username` VARCHAR(16) NOT NULL,
    `ip_address` VARCHAR(45) NOT NULL,
    `first_seen` BIGINT NOT NULL,
    `last_seen` BIGINT NOT NULL,
    `login_count` INT DEFAULT 1,
    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_ip` (`ip_address`),
    INDEX `idx_last_seen` (`last_seen`),
    UNIQUE KEY `unique_uuid_ip` (`uuid`, `ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rb_sync_events` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_type` ENUM('BAN', 'UNBAN', 'TEMPBAN', 'MUTE', 'UNMUTE', 'REVOKE', 'UPDATE') NOT NULL,
    `target_uuid` VARCHAR(36) NOT NULL,
    `punishment_id` BIGINT DEFAULT NULL,
    `data` TEXT,
    `server_id` VARCHAR(64) NOT NULL,
    `created_at` BIGINT NOT NULL,
    `processed` BOOLEAN DEFAULT FALSE,
    INDEX `idx_processed` (`processed`),
    INDEX `idx_created` (`created_at`),
    INDEX `idx_target` (`target_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rb_web_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` ENUM('ADMIN', 'MODERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    `created_at` BIGINT NOT NULL,
    `last_login` BIGINT DEFAULT NULL,
    `active` BOOLEAN DEFAULT TRUE,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rb_audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `action` VARCHAR(64) NOT NULL,
    `operator` VARCHAR(64) NOT NULL,
    `target` VARCHAR(36),
    `details` TEXT,
    `ip_address` VARCHAR(45),
    `timestamp` BIGINT NOT NULL,
    `source` ENUM('GAME', 'WEB', 'API') NOT NULL,
    INDEX `idx_timestamp` (`timestamp`),
    INDEX `idx_operator` (`operator`),
    INDEX `idx_target` (`target`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
