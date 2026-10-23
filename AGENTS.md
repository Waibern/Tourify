# Panduan untuk agen

## Ringkasan proyek

Tourify adalah aplikasi Android berbahasa Indonesia untuk melihat destinasi/wahana, memesan tiket, dan melihat tiket yang sudah dipesan. Aplikasi memakai Kotlin, Jetpack Compose, Navigation Compose, Retrofit, dan ViewModel.

## Sebelum mengubah kode

- Baca file pemilik fitur dan alur pemanggilnya terlebih dahulu. `MainActivity.kt` mengatur navigasi dan dependensi layar; jangan menambahkan rute atau state global tanpa memeriksa alur tersebut.
- Pertahankan perubahan sesempit mungkin dan ikuti pola yang sudah dipakai proyek. Jangan menganggap ada lapisan repository atau sistem sesi persisten; keduanya belum tampak di implementasi saat ini.
- Untuk kontrak API, cocokkan model Kotlin, `ApiService`, pemanggil di ViewModel, dan kontrak backend PHP yang tersedia. Jangan menebak nama field atau format tanggal.
- Pertahankan teks antarmuka dalam bahasa Indonesia dan gaya yang sudah ada: tata letak jelas, mudah dibaca, warna hijau-putih, dan dekorasi secukupnya.
- Jaga aksesibilitas Compose: label konten yang bermakna, dukungan ukuran teks besar, area klik yang wajar, serta state loading/error/kosong jika relevan.
- Saat menulis atau mengubah kode, jangan memadatkan blok atau komponen Compose bertingkat ke satu baris. Tulis kondisi dan isi blok ke bawah dengan indentasi yang jelas; pisahkan argumen pemanggilan, rantai `Modifier`, dan lambda panjang ke beberapa baris. Terapkan konsisten ke semua layar di `screen/`.
- Jangan menaruh kredensial, token, atau konfigurasi mesin pribadi ke source maupun dokumentasi. `local.properties` adalah konfigurasi lokal; jangan menyalin isinya ke commit.

## Peta kode

- `app/src/main/java/com/example/tourify_v2/MainActivity.kt` — splash, izin jaringan lokal, dan rute `login`, `register`, `home`, `ticket`, `account`.
- `app/src/main/java/com/example/tourify_v2/screen/` — layar Compose: login, daftar akun, beranda/pemesanan, tiket/QR, akun, dan navigasi bawah.
- `app/src/main/java/com/example/tourify_v2/viewmodel/` — operasi autentikasi dan pemuatan/pemesanan destinasi serta tiket.
- `app/src/main/java/com/example/tourify_v2/network/` — Retrofit client dan definisi endpoint.
- `app/src/main/java/com/example/tourify_v2/model/` — model respons API, wahana, tiket, dan permintaan pemesanan.
- `app/src/main/java/com/example/tourify_v2/ui/theme/` — tema, warna, dan tipografi.
- `app/src/main/res/drawable/` — logo, splash, dan gambar destinasi. Pemetaan gambar destinasi berdasarkan ID berada di `HomeScreen.kt`.
- `app/src/test/` dan `app/src/androidTest/` — pengujian unit dan Compose/instrumentasi yang sudah ada.

## API dan konfigurasi lokal

- `ApiClient.kt` berisi alamat dasar API yang saat ini diarahkan ke server MAMP lokal melalui HTTP. Periksa alamat yang aktif di file itu; jangan menyalin alamat IP mesin ke dokumen atau menganggap alamat tersebut berlaku di perangkat/jaringan lain.
- Endpoint yang dipakai: `register.php`, `login.php`, `wahana.php`, dan `tickets.php` (GET untuk daftar tiket, POST untuk pemesanan).
- Manifest mengizinkan internet, jaringan lokal, dan cleartext HTTP. Koneksi ke server lokal membutuhkan perangkat dan komputer server di jaringan yang sama; versi Android tertentu juga meminta izin jaringan lokal saat aplikasi dibuka.
- Jika mengubah alamat server, izin, skema HTTP/HTTPS, atau payload, jelaskan dampaknya dan pastikan sisi Android serta backend tetap cocok. Jangan menganggap pengaturan lokal ini sudah aman untuk rilis publik.

## Build dan verifikasi

Gunakan wrapper proyek dari direktori root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

Pilih pemeriksaan yang langsung relevan dengan perubahan dan permintaan pengguna. Pengujian instrumentasi memerlukan perangkat atau emulator yang tersedia. Laporkan hasil build, pengujian, dan pemeriksaan UI secara terpisah; build berhasil tidak membuktikan alur server atau tampilan di perangkat.

