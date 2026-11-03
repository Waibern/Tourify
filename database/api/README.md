# Tourify PHP API

Folder ini berisi API PHP lengkap untuk aplikasi Android dan dashboard. `db.php` membaca konfigurasi dari `/etc/tourify/database.php` atau path pada `TOURIFY_DB_CONFIG`; file konfigurasi itu harus berada di luar repo dan di luar document root. Contoh formatnya ada di `db.config.example.php`.

Jalankan PHP 8.3-FPM dengan ekstensi `pdo_mysql`, `curl`, `mbstring`, `xml`, `gd`, dan `fileinfo`. PHP-FPM juga perlu menerima `TOURIFY_DASHBOARD_ORIGIN` sesuai origin HTTPS dashboard serta `OPENAI_API_KEY` untuk permintaan chatbot. Simpan kedua nilai itu di konfigurasi server, jangan di Git atau Android.

Nginx harus meneruskan header login Android ke PHP-FPM dengan `fastcgi_param HTTP_AUTHORIZATION $http_authorization;`. Sesi dashboard memakai cookie `HttpOnly` dan `SameSite=Lax`; aktifkan HTTPS sebelum dashboard dibuka ke internet.

## Akses API

- `tickets.php` dan `chat.php` memerlukan `Authorization: Bearer ...` dari endpoint `login.php`. Server menentukan akun dari token, bukan dari username di request.
- Endpoint dashboard memakai sesi admin/petugas dari `dashboard_auth.php`.
- `wahana.php`, `register.php`, dan `login.php` tetap dipakai tanpa sesi.
- Foto profil disimpan di `uploads/profile/`. Simpan folder ini di storage persisten dan jangan ikut menghapusnya saat deploy.
- `cli/set_dashboard_role.php` hanya untuk CLI; blokir direktori `/cli/` di Nginx.

## Database

`../schema.sql` adalah skema dasar saat ini tanpa data pengguna. Untuk memindahkan data dari MAMP, buat backup terpisah lalu impor dump ke database baru di AWS. Dump penuh sudah membawa skema, jadi jangan jalankan ulang migrasi 001–005 di atasnya. Migrasi berikutnya dijalankan satu kali setelah backup.

Backup lokal MAMP tetap menjadi salinan pemulihan sampai data AWS dan alur aplikasi selesai diperiksa.
