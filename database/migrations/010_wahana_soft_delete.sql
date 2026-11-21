-- Arsip lunak menjaga tiket dan riwayat saat wahana dihapus dari katalog.
ALTER TABLE wahana
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_featured;
