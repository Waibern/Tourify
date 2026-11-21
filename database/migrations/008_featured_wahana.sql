-- Pilihan awal mempertahankan wahana unggulan yang sebelumnya dipilih aplikasi.
ALTER TABLE wahana ADD COLUMN is_featured TINYINT(1) NOT NULL DEFAULT 0;
UPDATE wahana SET is_featured = 1
WHERE id = COALESCE(
    (SELECT id FROM (SELECT id FROM wahana WHERE name = 'Pantai Mentari' ORDER BY id LIMIT 1) AS preferred),
    (SELECT id FROM (SELECT MIN(id) AS id FROM wahana) AS fallback)
);
