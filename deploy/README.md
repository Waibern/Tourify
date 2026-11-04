# Hosting Tourify di VPS AWS

Panduan ini menyiapkan staging publik dari repo, PHP-FPM, MySQL lokal VPS, dan dashboard Nuxt. Nilai IP dan rahasia tetap di server, tidak disimpan di Git. Hosting VPS tetap memakai PHP/MySQL; situs statis lama perlu diganti pada konfigurasi Nginx setelah dicadangkan.

## Kondisi sebelum deploy

- VPS ini memakai Lightsail. Lampirkan static IPv4 sebelum mengarahkan tester, lalu verifikasi aturan firewall Lightsail untuk TCP 80/443. Jangan membuka 3306; MySQL tetap di `127.0.0.1`.
- Simpan salinan konfigurasi Nginx lama, lalu nonaktifkan symlink `sites-enabled/default` saat mengaktifkan virtual host Tourify. Jangan hapus `/var/www/html` atau video lamanya.
- Repo GitHub ini privat. Gunakan deploy key khusus baca-saja untuk repo ini; pasang private key sebagai `/home/ubuntu/.ssh/tourify-vps-deploy` dengan permission `600`, lalu clone sebagai `ubuntu` agar update berikutnya bisa menarik perubahan. Contoh: `git -c core.sshCommand='ssh -i /home/ubuntu/.ssh/tourify-vps-deploy -o IdentitiesOnly=yes' clone git@github.com:Waibern/Tourify.git /var/www/tourify`, lalu simpan perintah SSH itu di konfigurasi lokal repo dengan `git -C /var/www/tourify config --local core.sshCommand 'ssh -i /home/ubuntu/.ssh/tourify-vps-deploy -o IdentitiesOnly=yes'`. Jangan pakai token pribadi yang punya akses tulis atau kirim private key lewat chat.
- Gunakan Node.js 24, pnpm 12.15.0 sesuai `web-dashboard/package.json`, PHP-FPM 8.3 dengan `pdo_mysql`, `curl`, `mbstring`, `xml`, `gd`, dan `fileinfo`, MySQL, serta Nginx.
- Sebelum mengecek pembaruan, jalankan `sudo apt update` agar daftar paket terbaru. `php -v` bisa tetap menampilkan 8.3.6 karena Ubuntu memakai versi backport; paket Noble `8.3.6-0ubuntu0.24.04.13` sudah memperbaiki CVE-2026-109618 dan CVE-2026-109619. Periksa versi penuh dengan `dpkg-query -W -f='${Version}\n' php8.3-fpm` dan kandidat dengan `apt-cache policy php8.3-fpm`; lihat [USN-8948-1](https://ubuntu.com/security/notices/USN-8948-1).
- Periksa juga CVE-2026-98432 pada kernel Linux melalui [status Ubuntu](https://ubuntu.com/security/CVE-2026-98432) dan [panduan Lightsail](https://docs.aws.amazon.com/lightsail/latest/userguide/address-cve-2026-98432.html). Saat audit 4 November 2026, kernel aktif `7.0.0-1012-aws`, sedangkan kandidat apt `7.0.0-1014.14~24.04.1`; Ubuntu masih mencatat perbaikan `linux-aws-7.0` sebagai sedang dikerjakan pada `7.0.0-1017.18~24.04.1`. Saat deploy, cek status dan kandidat terbaru, pasang kernel yang sudah dinyatakan fixed, reboot, lalu cocokkan `uname -r`. Jika belum ada kernel fixed, jangan buka port web publik sebelum mitigasi sementara AWS diterapkan dan diverifikasi; panduan AWS meminta snapshot sebelum perubahan firewall, dan aturan sementara tidak bertahan setelah reboot. Audit baca-saja 4 November 2026 menemukan `ufw status` inactive, kebijakan INPUT iptables default ACCEPT, dan ruleset nftables kosong; unit UFW terdaftar enabled, sedangkan nftables disabled. Jangan anggap firewall host sedang memfilter trafik, dan verifikasi aturan Lightsail secara terpisah. Sebelum memasang mitigasi, cek ulang kondisi ini dan pilih cara yang cocok dengan firewall aktif.
- Setelah pembaruan keamanan, cek ulang PHP-FPM, MySQL, dan Nginx serta apakah VPS perlu reboot.

## Database MAMP lengkap (jalur deploy yang dipilih)

Deploy ini menyalin database MAMP `tourify`, termasuk akun, tiket, wahana, dan catatan check-in. Ambil dump pada hari deploy supaya tiket terbaru ikut terbawa. Jangan ubah atau hapus database MAMP lokal.

- Sebelum menyalin, catat jumlah baris setiap tabel sumber dan pastikan tabel serta kolomnya cocok dengan kode PHP yang akan dipasang. Saat dicek, database berisi tabel `users`, `wahana`, `tickets`, dan `ticket_checkins`, dengan kolom terbaru yang dipakai oleh `database/schema.sql`.
- Jangan impor `database/schema.sql` atau `database/seed.sql` untuk jalur ini; keduanya hanya untuk staging kosong. Jangan menjalankan migrasi 001–005 di atas dump yang sudah memiliki skema terbaru.
- Buat database tujuan baru dengan nama sementara yang unik, lalu beri akun aplikasi akses hanya ke database itu dari `127.0.0.1`. Biarkan MySQL tetap terikat ke alamat lokal itu.
- Buat dump konsisten dengan `mysqldump --single-transaction --routines --triggers --events --hex-blob --default-character-set=utf8mb4 --no-tablespaces`, lalu kirim sebagai stream melalui SSH terenkripsi langsung ke MySQL VPS. Jangan menyimpan dump plaintext di disk, memasukkannya ke Git, atau mengirim isi database lewat chat. Pakai prompt password, bukan argumen password di command line.
- Untuk database tujuan contoh `tourify_mamp_import`, alirkan `mysqldump` yang memakai koneksi MAMP aktif ke `gzip`, lalu `ssh` ke VPS dan impor hasil dekompresi dengan `sudo mysql --database=tourify_mamp_import`. Jika nama itu sudah ada, pilih nama unik lain dan jangan timpa. Aktifkan `set -o pipefail` di shell lokal dan shell remote agar kegagalan dump atau impor terlihat. Isi host, port, user, kunci SSH, dan IPv4 dari konfigurasi yang aktif saat deploy; gunakan opsi `--password` supaya mysqldump meminta password secara interaktif.
- Salin juga foto profil yang dirujuk `users.profile_image` dari folder upload MAMP ke `/var/lib/tourify/uploads/profile` melalui SSH, dengan nama file dan path relatif tetap. Verifikasi setiap referensi foto punya file tujuan sebelum mengalihkan konfigurasi.
- Bandingkan jumlah baris sumber dan tujuan untuk semua tabel, lalu cek login, dashboard, daftar wahana, pemesanan, dan tiket. Jangan alihkan `/etc/tourify/database.php` ke database hasil impor sebelum jumlah baris dan pemeriksaan alur cocok.
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
