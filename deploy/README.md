# Hosting Tourify di VPS AWS

Panduan ini menyiapkan staging publik dari repo, PHP-FPM, MySQL lokal VPS, dan dashboard Nuxt. Nilai IP dan rahasia tetap di server, tidak disimpan di Git. Hosting VPS tetap memakai PHP/MySQL; situs statis lama perlu diganti pada konfigurasi Nginx setelah dicadangkan.

## Kondisi sebelum deploy

- Pastikan public IPv4 yang dipakai tetap stabil. Verifikasi Elastic IP dan aturan Security Group untuk TCP 80/443 di AWS Console. Jangan membuka 3306; MySQL tetap di `127.0.0.1`.
- Simpan salinan konfigurasi Nginx lama, lalu nonaktifkan symlink `sites-enabled/default` saat mengaktifkan virtual host Tourify. Jangan hapus `/var/www/html` atau video lamanya.
- Repo GitHub ini privat. Sebelum clone ke `/var/www/tourify`, VPS perlu deploy key khusus dengan akses baca-saja ke repo ini; jangan pakai token pribadi yang punya akses tulis.
- Gunakan Node.js, pnpm, PHP-FPM 8.3 dengan `pdo_mysql`, `curl`, `mbstring`, `xml`, `gd`, dan `fileinfo`, MySQL, serta Nginx.

## Database staging baru

Buat database `tourify` dan akun MySQL khusus aplikasi yang hanya boleh tersambung dari lokal. Pasang file konfigurasi berdasarkan `database/api/db.config.example.php` di `/etc/tourify/database.php`, di luar repo, dengan pemilik `root:www-data` dan mode `640`.

Untuk database staging kosong, impor `database/schema.sql` lalu `database/seed.sql`. Skema tersebut sudah memuat kolom terbaru; seed hanya menambah lima wahana, tanpa akun atau tiket. Jangan impor dump MAMP atau menjalankan migrasi 001–005 di atas skema ini. Salinan MAMP lokal tetap utuh.

Setelah membuat akun lewat halaman daftar Tourify, ubah role akun yang dipilih menjadi `admin` memakai skrip CLI `database/api/cli/set_dashboard_role.php`. Jangan layani folder `/cli/` lewat Nginx.

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
- `/tourify-api/wahana.php` menampilkan lima data seed; `/tourify-api/` dan `/tourify-api/cli/` tidak membuka file.
- Daftar akun, login, pemesanan tiket, dan tampilan tiket tersimpan di MySQL berjalan dari jaringan luar melalui HTTPS.
- Login dashboard hanya menerima akun ber-role `admin` atau `gate`; akun awal dipilih dan diberi role secara sadar.
- MySQL tetap lokal dan tidak ada aturan Security Group untuk port 3306.
