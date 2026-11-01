# Endpoint daftar admin

Salin `dashboard_orders.php`, `dashboard_checkins.php`, `dashboard_customers.php`, dan `profile.php` ke direktori backend Tourify yang sudah memiliki `common.php` dan `db.php`. Endpoint memakai koneksi serta sesi dashboard yang ada. Untuk foto profil aplikasi, `common.php` dan `login.php` juga perlu menyediakan serta mengembalikan token akses; perubahan lokal sudah ada di direktori API MAMP. Tidak ada kredensial dalam berkas ini.

Memerlukan skema dari migrasi 001–004. Endpoint pelanggan, pesanan, dan check-in hanya membaca data untuk role `admin`; daftar pelanggan juga mengembalikan path foto akun. Profil memakai sesi dashboard `admin`/`gate` atau token aplikasi hasil login. `profile.php` menyediakan GET untuk membaca path foto dan POST multipart dengan field `image` untuk mengunggah foto JPG, PNG, atau WebP maksimal 5 MB. File disimpan di `uploads/profile/` dan hanya path-nya yang ditulis ke tabel `users`.

Untuk notifikasi langsung, salin juga `dashboard_activity.php` dan `dashboard_activity_feed.php`. GET pertama mengambil penanda awal tanpa mengirim aktivitas lama. Polling berikutnya memakai `after_order` dan `after_customer` dari `cursor` respons sebelumnya. Feed admin membaca maksimal 20 pesanan dan 20 akun pelanggan per request, tanpa filter tanggal halaman dan tanpa migrasi tambahan. Dashboard memeriksa setiap 5 detik saat tab terlihat; kartu menutup setelah 3 detik dan timer berhenti sementara saat diarahkan atau difokuskan.

Parameter daftar GET: `q`, `date` (`YYYY-MM-DD`), dan `page` (mulai 1). Pesanan juga menerima `status`: `Active`, `Used`, atau `Expire`. Hasil dibatasi 20 baris per halaman; waktu check-in memakai WIB.

Jalankan pemeriksaan integrasi terhadap backend yang sudah dikonfigurasi:

```sh
python3 database/tests/check_dashboard_lists.py /path/to/tourify-api /path/to/php
```

Pemeriksaan memakai sesi CLI sementara, tidak mengubah data pesanan atau sesi browser.

Tes feed yang mencakup patokan awal, ID ganda, dan lonjakan aktivitas memakai SQLite di memori:

```sh
php database/tests/check_dashboard_activity.php
```
