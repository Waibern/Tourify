-- Tambah status Terpakai kalau semua tiket dalam pesanan sudah discan.
ALTER TABLE `tickets`
    MODIFY COLUMN `status` ENUM('Active', 'Expire', 'Used') NOT NULL DEFAULT 'Active';

-- Perbarui status tiket lama dari jumlah scan dan tanggal Jakarta.
UPDATE `tickets`
SET `status` = CASE
    WHEN `checked_in_quantity` >= `quantity` THEN 'Used'
    WHEN `visit_date` < DATE(DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 HOUR)) THEN 'Expire'
    ELSE 'Active'
END;
