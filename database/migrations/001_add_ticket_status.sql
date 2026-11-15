-- Jalankan sekali di database Tourify yang sudah punya tabel tickets.
ALTER TABLE `tickets`
    ADD COLUMN `status` ENUM('Active', 'Expire') NOT NULL DEFAULT 'Active' AFTER `visit_date`;

-- Sesuaikan status tiket lama dengan tanggal Jakarta (UTC+7).
UPDATE `tickets`
SET `status` = IF(
    `visit_date` < DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 HOUR)),
    'Expire',
    'Active'
);
