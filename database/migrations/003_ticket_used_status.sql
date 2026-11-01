-- Tambah status terpakai untuk pesanan yang semua tiketnya sudah check-in.
ALTER TABLE `tickets`
    MODIFY COLUMN `status` ENUM('Active', 'Expire', 'Used') NOT NULL DEFAULT 'Active';

-- Selaraskan tiket lama dengan jumlah scan yang sudah tercatat dan tanggal Jakarta.
UPDATE `tickets`
SET `status` = CASE
    WHEN `checked_in_quantity` >= `quantity` THEN 'Used'
    WHEN `visit_date` < DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 HOUR)) THEN 'Expire'
    ELSE 'Active'
END;
