-- Sembunyikan tiket yang sudah selesai dari riwayat akun tanpa menghapus catatan booking.
ALTER TABLE `tickets`
    ADD COLUMN `history_hidden_at` TIMESTAMP NULL DEFAULT NULL AFTER `payment_method`;

-- Untuk membatalkan perubahan ini sebelum dipakai:
-- ALTER TABLE `tickets` DROP COLUMN `history_hidden_at`;
