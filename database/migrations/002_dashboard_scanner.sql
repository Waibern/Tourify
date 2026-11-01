-- Tambah role dashboard dan pencatatan masuk per orang tanpa mengubah status tiket aplikasi.
ALTER TABLE `users`
    ADD COLUMN `role` ENUM('customer', 'admin', 'gate') NOT NULL DEFAULT 'customer' AFTER `password_hash`;

ALTER TABLE `tickets`
    ADD COLUMN `checked_in_quantity` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `quantity`;

CREATE TABLE `ticket_checkins` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `ticket_id` BIGINT UNSIGNED NOT NULL,
    `staff_user_id` BIGINT UNSIGNED NOT NULL,
    `scanned_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_checkins_ticket_id` (`ticket_id`),
    KEY `idx_ticket_checkins_staff_user_id` (`staff_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
