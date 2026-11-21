-- Wahana yang sudah ada tetap bisa memakai gambar bawaan.
ALTER TABLE wahana ADD COLUMN image_path VARCHAR(255) DEFAULT NULL;
