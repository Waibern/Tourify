-- Catat aktivitas terakhir pelanggan untuk indikator online dashboard.
ALTER TABLE `users`
    ADD COLUMN `last_seen_at` TIMESTAMP NULL DEFAULT NULL AFTER `created_at`,
    ADD KEY `idx_users_role_last_seen_at` (`role`, `last_seen_at`);

-- Rollback: ALTER TABLE `users` DROP INDEX `idx_users_role_last_seen_at`, DROP COLUMN `last_seen_at`;
