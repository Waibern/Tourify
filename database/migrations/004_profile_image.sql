-- Simpan lokasi foto supaya aplikasi dan dashboard memakai gambar yang sama.
ALTER TABLE `users`
    ADD COLUMN `profile_image` VARCHAR(255) NULL AFTER `username`;

-- Untuk membatalkan sebelum foto baru dipakai, jalankan:
-- ALTER TABLE `users` DROP COLUMN `profile_image`;
