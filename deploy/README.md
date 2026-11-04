# Hosting Tourify di VPS AWS

Panduan ini menyiapkan staging publik dari repo, PHP-FPM, MySQL lokal VPS, dan dashboard Nuxt. Nilai IP dan rahasia tetap di server, tidak disimpan di Git. Hosting VPS tetap memakai PHP/MySQL; situs statis lama perlu diganti pada konfigurasi Nginx setelah dicadangkan.

## Kondisi sebelum deploy

- VPS ini memakai Lightsail. Audit baca-saja 4 November 2026 menemukan static IPv4 belum terpasang; firewall Lightsail mengizinkan port 80 dan 22 dari semua IPv4/IPv6, sedangkan 443 belum dibuka. Periksa ulang status ini saat deploy, lampirkan static IPv4, lalu buka HTTPS setelah gerbang keamanan kernel siap. Jangan membuka 3306; MySQL tetap di `127.0.0.1`.
- Simpan salinan konfigurasi Nginx lama, lalu nonaktifkan symlink `sites-enabled/default` saat mengaktifkan virtual host Tourify. Jangan hapus `/var/www/html` atau video lamanya.
- Repo GitHub ini privat. Gunakan deploy key khusus baca-saja untuk repo ini; pasang private key sebagai `/home/ubuntu/.ssh/tourify-vps-deploy` dengan permission `600`, lalu clone sebagai `ubuntu` agar update berikutnya bisa menarik perubahan. Contoh: `git -c core.sshCommand='ssh -i /home/ubuntu/.ssh/tourify-vps-deploy -o IdentitiesOnly=yes' clone git@github.com:Waibern/Tourify.git /var/www/tourify`, lalu simpan perintah SSH itu di konfigurasi lokal repo dengan `git -C /var/www/tourify config --local core.sshCommand 'ssh -i /home/ubuntu/.ssh/tourify-vps-deploy -o IdentitiesOnly=yes'`. Jangan pakai token pribadi yang punya akses tulis atau kirim private key lewat chat.
- Gunakan Node.js 24, pnpm 12.15.0 sesuai `web-dashboard/package.json`, PHP-FPM 8.3 dengan `pdo_mysql`, `curl`, `mbstring`, `xml`, `gd`, dan `fileinfo`, MySQL, serta Nginx.
- Sebelum mengecek pembaruan, jalankan `sudo apt update` agar daftar paket terbaru. `php -v` bisa tetap menampilkan 8.3.6 karena Ubuntu memakai versi backport; paket Noble `8.3.6-0ubuntu0.24.04.13` sudah memperbaiki CVE-2026-109618 dan CVE-2026-109619. Periksa versi penuh dengan `dpkg-query -W -f='${Version}\n' php8.3-fpm` dan kandidat dengan `apt-cache policy php8.3-fpm`; lihat [USN-8948-1](https://ubuntu.com/security/notices/USN-8948-1).
- Periksa CVE-2026-98432 pada kernel lewat [status Ubuntu](https://ubuntu.com/security/CVE-2026-98432) dan [panduan Lightsail](https://docs.aws.amazon.com/lightsail/latest/userguide/address-cve-2026-98432.html). Audit 4 November 2026 menemukan kernel aktif `7.0.0-1012-aws` dan kandidat apt `7.0.0-1014.14~24.04.1`; status Ubuntu terkini masih menyebut perbaikan `linux-aws-7.0` dalam pengerjaan pada `7.0.0-1017.18~24.04.1`. Cek ulang status resmi dan paket pada hari deploy. Jika versi fixed sudah tersedia, pasang, reboot, lalu pastikan `uname -r` menunjukkan kernel fixed sebelum situs dibuka.
- Jika kernel fixed belum tersedia, panduan AWS menyarankan snapshot Lightsail sebelum perubahan firewall, lalu aturan iptables sementara untuk Ubuntu: `sudo iptables -I INPUT 1 -p tcp -m u32 --u32 '0>>24&0x0f=6:15' -j DROP`. Pastikan `iptables` dan pencocok `u32` tersedia, lalu verifikasi aturan pertama dengan `sudo iptables -L INPUT -n -v --line-numbers`. Aturan ini hanya mitigasi sementara dan hilang saat reboot; setelah kernel fixed aktif, hapus dengan `sudo iptables -D INPUT -p tcp -m u32 --u32 '0>>24&0x0f=6:15' -j DROP`. Jika perintah atau aturan tidak cocok dengan firewall aktif, ikuti panduan AWS dan jangan membuka website publik sampai mitigasi terverifikasi.
- Audit host 4 November 2026 sebelumnya menemukan `ufw status` inactive, INPUT iptables default ACCEPT, dan ruleset nftables kosong. Laporan Hermes berikutnya hanya menyebut service UFW aktif tetapi tidak enabled saat boot, tanpa output aturan yang memastikan status runtime. Karena hasilnya berbeda, periksa `sudo -n ufw status verbose`, `sudo -n iptables -S INPUT`, `sudo -n nft list ruleset`, dan status systemd firewall pada hari deploy. Jangan menganggap firewall host sedang memfilter trafik; periksa aturan Lightsail secara terpisah. Jika `sudo -n` gagal, jangan kirim password lewat chat.
- Setelah pembaruan keamanan, cek ulang PHP-FPM, MySQL, dan Nginx serta apakah VPS perlu reboot.

## Database MAMP lengkap (jalur deploy yang dipilih)

Deploy ini menyalin database MAMP `tourify`, termasuk akun, tiket, wahana, dan catatan check-in. Ambil dump pada hari deploy supaya tiket terbaru ikut terbawa. Jangan ubah atau hapus database MAMP lokal.

- Sebelum menyalin, catat jumlah baris setiap tabel sumber dan pastikan tabel serta kolomnya cocok dengan kode PHP yang akan dipasang. Audit baca-saja 4 November 2026 menemukan `users` 7 baris, `wahana` 5, `tickets` 34, `ticket_checkins` 9; semua tabel memakai InnoDB dan tidak ada relasi tiket/check-in yang yatim. Ulangi pemeriksaan ini pada hari deploy.
- Jangan impor `database/schema.sql` atau `database/seed.sql` untuk jalur ini; keduanya hanya untuk staging kosong. Jangan menjalankan migrasi 001–005 di atas dump yang sudah memiliki skema terbaru.
- Buat database tujuan baru dengan nama sementara yang unik, lalu beri akun aplikasi akses hanya ke database itu dari `127.0.0.1`. Biarkan MySQL tetap terikat ke alamat lokal itu.
- Buat dump konsisten dengan `mysqldump --single-transaction --routines --triggers --events --hex-blob --default-character-set=utf8mb4 --no-tablespaces`, lalu kirim sebagai stream melalui SSH terenkripsi langsung ke MySQL VPS. Jangan menyimpan dump plaintext di disk, memasukkannya ke Git, atau mengirim isi database lewat chat. Pakai prompt password, bukan argumen password di command line.
- Untuk database tujuan contoh `tourify_mamp_import`, alirkan `mysqldump` yang memakai koneksi MAMP aktif ke `gzip`, lalu `ssh` ke VPS dan impor hasil dekompresi dengan `sudo mysql --database=tourify_mamp_import`. Jika nama itu sudah ada, pilih nama unik lain dan jangan timpa. Aktifkan `set -o pipefail` di shell lokal dan shell remote agar kegagalan dump atau impor terlihat. Isi host, port, user, kunci SSH, dan IPv4 dari konfigurasi yang aktif saat deploy; gunakan opsi `--password` supaya mysqldump meminta password secara interaktif.
- Salin juga foto profil yang dirujuk `users.profile_image` dari folder upload MAMP ke `/var/lib/tourify/uploads/profile` melalui SSH, dengan nama file dan path relatif tetap. Verifikasi setiap referensi foto punya file tujuan sebelum mengalihkan konfigurasi.
- Jalankan query berikut pada database sumber dan tujuan; jumlah baris harus sama dan semua hitungan relasi yatim harus nol. Lalu cek login, dashboard, daftar wahana, pemesanan, dan tiket. Jangan alihkan `/etc/tourify/database.php` ke database hasil impor sebelum hasilnya cocok.

```sql
SELECT 'users' AS tabel, COUNT(*) AS jumlah FROM users
UNION ALL SELECT 'wahana', COUNT(*) FROM wahana
UNION ALL SELECT 'tickets', COUNT(*) FROM tickets
UNION ALL SELECT 'ticket_checkins', COUNT(*) FROM ticket_checkins;

SELECT
  (SELECT COUNT(*) FROM tickets t LEFT JOIN users u ON u.id = t.user_id WHERE u.id IS NULL) AS tiket_tanpa_akun,
  (SELECT COUNT(*) FROM tickets t LEFT JOIN wahana w ON w.id = t.wahana_id WHERE w.id IS NULL) AS tiket_tanpa_wahana,
  (SELECT COUNT(*) FROM ticket_checkins c LEFT JOIN tickets t ON t.id = c.ticket_id WHERE t.id IS NULL) AS checkin_tanpa_tiket,
  (SELECT COUNT(*) FROM ticket_checkins c LEFT JOIN users u ON u.id = c.staff_user_id WHERE u.id IS NULL) AS checkin_tanpa_petugas;
```
- Pemeriksaan baca-saja terakhir menemukan belum ada database aplikasi di VPS. Buat database baru dengan nama unik untuk setiap percobaan dan jangan timpa database yang sudah ada. Simpan database MAMP lokal tetap utuh. Kalau pemeriksaan gagal, pulihkan konfigurasi situs dan Nginx sebelumnya; biarkan database hasil impor tetap ada untuk diperiksa. Bila database aplikasi lama sudah ada saat deploy, cadangkan dan pertahankan sebelum mengalihkan konfigurasi, agar bisa dipakai untuk rollback.

Pasang file konfigurasi berdasarkan `database/api/db.config.example.php` di `/etc/tourify/database.php`, di luar repo, dengan pemilik `root:www-data` dan mode `640`. Pastikan database tujuan sudah berisi akun `admin` atau `gate` yang memang dipilih; jangan menaikkan role akun secara otomatis. Jangan layani folder `/cli/` lewat Nginx.

### Database staging kosong

Hanya untuk staging baru tanpa data MAMP: impor `database/schema.sql`, lalu `database/seed.sql`. Seed hanya menambah lima wahana, tanpa akun atau tiket. Buat database `tourify` dan akun MySQL khusus aplikasi yang hanya boleh tersambung dari lokal.

## Dashboard, API, dan foto profil

- Pasang `deploy/tourify-dashboard.service` sebagai `/etc/systemd/system/tourify-dashboard.service`. Unit memakai Node di `/usr/bin/node` dan Nginx hanya mem-proxy ke `127.0.0.1:3000`.
- Build pertama dari `web-dashboard` memakai `pnpm install --frozen-lockfile` lalu `pnpm build`. Service Nuxt hanya mendengarkan `127.0.0.1:3000`.
- Buat `/var/www/certbot`, pasang `deploy/nginx/tourify-issue-cert.conf` sebagai virtual host sementara, lalu terbitkan sertifikat IP. Setelah itu pasang `deploy/nginx/tourify.conf.template`.
- Nginx harus meneruskan endpoint PHP yang tercantum di template ke socket PHP-FPM, meneruskan `HTTP_AUTHORIZATION`, dan memberi `HTTPS=on`. Endpoint lain di `/tourify-api/`, termasuk `/cli/`, harus 404.
- Foto dapat berukuran sampai 5 MB. Template Nginx menetapkan `client_max_body_size 6m`; pasang `deploy/php-fpm/99-tourify.ini` ke `/etc/php/8.3/fpm/conf.d/` agar PHP-FPM memakai `upload_max_filesize=5M` dan `post_max_size=6M`, lalu reload layanan.
- Buat `/var/lib/tourify/uploads/profile` untuk file foto persisten, lalu jadikan `database/api/uploads/profile` symlink ke folder tersebut. Pastikan `www-data` dapat menulis dan Nginx membacanya.
- Di pool PHP-FPM `[www]`, set `env[TOURIFY_DASHBOARD_ORIGIN]` ke origin HTTPS yang dipakai, lalu reload PHP-FPM. `OPENAI_API_KEY` hanya diperlukan untuk pertanyaan chatbot yang memakai AI; bila dipakai, simpan di konfigurasi privat PHP-FPM, bukan di repo atau Android.

## HTTPS tanpa domain

Let's Encrypt sekarang menerbitkan sertifikat untuk alamat IP. Certbot harus versi 5.4 atau lebih baru; sertifikat IP memakai profil `shortlived` dan berlaku sekitar enam hari, jadi pembaruan otomatis wajib. Certbot dapat menerbitkan sertifikatnya, tetapi Nginx perlu diarahkan manual ke file sertifikat dan hook pembaruan harus me-reload Nginx. Lihat [panduan IP certificate Certbot dari Let's Encrypt](https://letsencrypt.org/2026/03/11/shorter-certs-certbot) dan [masa berlaku profil](https://letsencrypt.org/docs/profiles/).

VPS sudah memiliki `snapd` aktif. Saat audit 4 November 2026, Certbot belum terpasang dan kandidat apt masih `2.9.0-1`, terlalu lama untuk sertifikat IP. Cek lagi pada hari deploy, lalu gunakan [paket Snap resmi Certbot](https://certbot.eff.org/instructions?os=snap&ws=nginx):

```bash
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/local/bin/certbot
certbot --version
```

Pastikan versi yang tampil 5.4 atau lebih baru sebelum menerbitkan sertifikat. Jangan memasang versi apt yang lebih lama.

Sebelum memakai template HTTPS, terbitkan sertifikat dengan nama `tourify` menggunakan webroot `/var/www/certbot` dan public IPv4 yang sudah dipastikan stabil. Pastikan timer `certbot renew` aktif dan deploy hook me-reload Nginx; lakukan `certbot renew --dry-run` sebelum membuka alamat ke tester.

Perintah penerbitan setelah Certbot 5.4+ tersedia:

```bash
PUBLIC_IPV4='<alamat IPv4 dari AWS Console>'
sudo certbot certonly --cert-name tourify --preferred-profile shortlived --webroot --webroot-path /var/www/certbot --ip-address "$PUBLIC_IPV4"
```

Pasang `deploy/certbot-reload-nginx.sh` di `/etc/letsencrypt/renewal-hooks/deploy/` dengan permission executable. Jika Certbot meminta persetujuan syarat layanan atau email, pemilik akun menyelesaikan prompt tersebut.

## Update setelah perubahan web

1. Commit dan push perubahan dari Mac ke `main`.
2. Di VPS, jalankan `deploy/update-vps.sh` dari checkout Tourify.

Script hanya fast-forward dari `origin/main`, memasang dependency sesuai lockfile, build Nuxt, restart service, lalu cek halaman login di localhost. Script tidak menjalankan migrasi database; perubahan skema harus punya backup dan langkah migrasi terpisah.

## Pemeriksaan saat go-live

- `nginx -t` lolos, service dashboard aktif, dan HTTPS menampilkan sertifikat untuk IP yang tepat.
- `/tourify-api/wahana.php` menampilkan data wahana hasil impor; `/tourify-api/` dan `/tourify-api/cli/` tidak membuka file.
- Daftar akun, login, pemesanan tiket, dan tampilan tiket tersimpan di MySQL berjalan dari jaringan luar melalui HTTPS.
- Login dashboard hanya menerima akun ber-role `admin` atau `gate`; akun awal dipilih dan diberi role secara sadar.
- MySQL tetap lokal dan firewall Lightsail tidak memiliki aturan untuk port 3306.
