-- Simpan path foto agar aplikasi dan dashboard membaca gambar yang sama.
ALTER TABLE `users`
    ADD COLUMN `profile_image` VARCHAR(255) NULL AFTER `username`;

-- Rollback sebelum foto baru dipakai: ALTER TABLE `users` DROP COLUMN `profile_image`;
