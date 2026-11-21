# Tourify PHP API

Folder ini berisi API PHP lengkap untuk aplikasi Android dan dashboard. `db.php` membaca konfigurasi dari `/etc/tourify/database.php` atau path pada `TOURIFY_DB_CONFIG`; file konfigurasi itu harus berada di luar repo dan di luar document root. Contoh formatnya ada di `db.config.example.php`.

Jalankan PHP 8.3-FPM dengan ekstensi `pdo_mysql`, `curl`, `mbstring`, `xml`, `gd`, dan `fileinfo`. PHP-FPM juga perlu menerima `TOURIFY_DASHBOARD_ORIGIN` sesuai origin HTTPS dashboard serta `OPENAI_API_KEY` untuk permintaan chatbot. Simpan kedua nilai itu di konfigurasi server, jangan di Git atau Android.

Nginx harus meneruskan header login Android ke PHP-FPM dengan `fastcgi_param HTTP_AUTHORIZATION $http_authorization;`. Sesi dashboard memakai cookie `HttpOnly` dan `SameSite=Lax`; aktifkan HTTPS sebelum dashboard dibuka ke internet.

## Akses API

- `tickets.php`, `ratings.php`, dan `chat.php` memerlukan `Authorization: Bearer ...` dari endpoint `login.php`. Server menentukan akun dari token, bukan dari username di request.
- `ratings.php` menerima `ticket_id` dan `rating` 1–5. Tiket harus milik akun dari token dan sudah memiliki check-in; rating booking yang sama bisa diperbarui.
- `tickets.php` mengirim rating tersimpan per booking, sedangkan `wahana.php` mengirim rata-rata rating atau `null` jika belum ada rating.
- Endpoint dashboard memakai sesi admin/petugas dari `dashboard_auth.php`.
- `wahana.php`, `register.php`, dan `login.php` tetap dipakai tanpa sesi.
- Foto profil disimpan di `uploads/profile/`. Simpan folder ini di storage persisten dan jangan ikut menghapusnya saat deploy.
- `cli/set_dashboard_role.php` hanya untuk CLI; blokir direktori `/cli/` di Nginx.

## Database

`../schema.sql` adalah skema dasar saat ini tanpa data pengguna. Untuk memindahkan data dari MAMP, buat backup terpisah lalu impor dump ke database baru di AWS. Dump penuh sudah membawa skema, jadi jangan jalankan ulang migrasi yang sudah diterapkan. Jalankan `../migrations/007_add_ticket_ratings.sql` satu kali sebelum deploy API rating ke database lama.

Untuk database staging baru tanpa akun/tiket lama, impor `../schema.sql` lalu `../seed.sql`. Seed hanya mengisi katalog wahana dan aman dijalankan ulang.

Backup lokal MAMP tetap menjadi salinan pemulihan sampai data AWS dan alur aplikasi selesai diperiksa.

## Wahana unggulan

Admin memilih satu unggulan lewat tombol **Jadikan unggulan** di halaman Wahana. `POST dashboard_wahana.php` menerima `id` wahana dan menggantikan pilihan sebelumnya. API katalog mengirim boolean `is_featured`; aplikasi memakainya untuk banner Beranda. Terapkan migrasi `008_featured_wahana.sql` sebelum deploy API ini ke database lama.


## Kelola wahana dan foto

Terapkan migrasi `009_wahana_image.sql` sebelum memakai API baru. Admin dapat mengirim multipart `POST dashboard_wahana.php?action=create` untuk membuat wahana atau `?action=update` untuk memperbaruinya. Kirim `name`, `description`, `ticket_price`, dan `id` (khusus update). Field file `image` opsional menerima JPG, PNG, dan WebP hingga 5 MB. Foto sebelumnya tetap tersimpan jika edit dikirim tanpa file. Harga form dibatasi Rp107.374.182 agar total untuk 20 tiket muat di aplikasi Android.

Foto disimpan di `uploads/wahana/`. API dashboard dan katalog publik mengembalikan path relatif pada `image_path`. Pastikan PHP bisa menulis ke folder tersebut dan server web melayaninya sebagai aset statis.

Penghapusan wahana memakai arsip lunak. Terapkan migrasi `010_wahana_soft_delete.sql` sebelum memasang API baru. Wahana yang dihapus tidak lagi tampil di katalog atau pilihan pemesanan baru; seluruh tiket, check-in, rating, dan foto lama tetap tersimpan agar riwayat pelanggan tetap utuh. Dialog dashboard menampilkan jumlah pemilik dan tiket terkait sebagai peringatan sebelum arsip.

Untuk rollback, pasang kembali API dan dashboard versi sebelumnya terlebih dahulu, lalu jalankan `ALTER TABLE wahana DROP COLUMN is_deleted;`. Baris wahana yang diarsipkan akan tampil lagi; data tiket dan foto tidak dihapus.
