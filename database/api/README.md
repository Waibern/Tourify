# Endpoint daftar admin

Salin `dashboard_checkin.php`, `dashboard_orders.php`, `dashboard_checkins.php`, `dashboard_customers.php`, `dashboard_members.php`, `dashboard_security.php`, `customer_presence.php`, dan `profile.php` ke direktori backend Tourify yang sudah memiliki `common.php` dan `db.php`. Endpoint check-in mengunci baris tiket selama transaksi supaya dua scan bersamaan tidak melewati jumlah pengunjung yang dibeli. Endpoint memakai koneksi serta sesi dashboard yang ada. Untuk foto profil, heartbeat pelanggan, dan perubahan role aktif, backend juga memakai perubahan `common.php` dan `login.php` di direktori API MAMP. Tidak ada kredensial dalam berkas ini.

Memerlukan skema dari migrasi 001–005. Endpoint pelanggan, pesanan, check-in, dan anggota hanya bisa dipakai role `admin`; daftar pelanggan juga mengembalikan path foto akun dan jumlah yang mengirim heartbeat dalam lima menit terakhir. `dashboard_members.php` mencantumkan akun admin/petugas melalui GET, membuat akun lewat POST, mengubah peran lewat PUT, dan menghapus anggota lewat DELETE. Perubahan dan penghapusan role menjaga admin terakhir; akun dengan pesanan atau riwayat check-in tidak bisa dihapus. `dashboard_security.php` memeriksa password saat ini sebelum mengganti password lewat POST atau menghapus akun sendiri lewat DELETE, lalu mengakhiri sesi. `common.php` membaca ulang role dari database agar perubahan peran langsung membatasi akses sesi yang aktif. Tidak ada migrasi skema tambahan. Aplikasi mengirim heartbeat pelanggan setiap 60 detik saat aktif. `dashboard_customers.php` menerima DELETE JSON `{ "ids": [1, 2] }` untuk menghapus beberapa akun sekaligus, tetapi menolak seluruh permintaan jika ada akun yang tidak valid atau punya pesanan. Profil memakai sesi dashboard `admin`/`gate` atau token aplikasi hasil login. `profile.php` menyediakan GET untuk membaca profil, PUT untuk mengubah username dashboard, serta POST multipart untuk mengunggah foto JPG, PNG, atau WebP maksimal 5 MB; POST dapat membawa username dashboard bersama foto. File disimpan di `uploads/profile/` dan hanya path-nya yang ditulis ke tabel `users`.

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
