# Konteks proyek Tourify

## Tujuan aplikasi

Tourify adalah aplikasi Android untuk menjelajahi wahana/destinasi dan membeli tiket kunjungan. Pengguna dapat mendaftar dan login, mencari destinasi, memilih tanggal kunjungan serta jumlah tiket dan metode pembayaran, lalu membuka riwayat tiket yang menampilkan QR.

## Teknologi dan bentuk aplikasi

- Kotlin dan Jetpack Compose untuk antarmuka.
- Navigation Compose untuk perpindahan layar.
- ViewModel dan coroutine untuk state layar serta panggilan jaringan.
- Retrofit dengan Gson untuk API; OkHttp logging interceptor memakai level BASIC.
- Namespace dan application ID: `com.example.tourify_v2`.
- `minSdk` 29, `targetSdk` 37, `compileSdk` 37, Java compatibility 11.
- Versi library dan plugin dikelola lewat `gradle/libs.versions.toml`.

## Alur layar

1. `MainActivity` menampilkan splash singkat, lalu meminta izin jaringan lokal pada Android API 37 ke atas jika belum diberikan.
2. Aplikasi dimulai dari `login`. Pengguna dapat membuka `register`; setelah registrasi berhasil, aplikasi kembali ke login.
3. Login berhasil membuka `home`. Navigasi bawah berpindah di antara `home`, `ticket`, dan `account`.
4. Beranda memuat daftar wahana, mendukung pencarian, dan membuka dialog pemesanan.
5. Layar tiket memuat riwayat berdasarkan username dan menampilkan kartu tiket dengan QR.
6. Layar akun menampilkan username dan menyediakan logout.

## Struktur data dan jaringan

`ApiService` mendefinisikan endpoint berikut:

| Operasi | Endpoint | Bentuk |
| --- | --- | --- |
| Daftar akun | `register.php` | POST kredensial |
| Login | `login.php` | POST kredensial |
| Daftar wahana | `wahana.php` | GET |
| Riwayat tiket | `tickets.php?username=...` | GET |
| Pesan tiket | `tickets.php` | POST data pemesanan |

Alamat dasar API saat ini ditulis langsung di `network/ApiClient.kt` dan menunjuk ke server MAMP lokal melalui HTTP. Periksa file tersebut untuk nilai yang berlaku di checkout ini. Komputer server dan perangkat Android perlu berada pada jaringan yang dapat saling menjangkau; pengaturan lokal tersebut bukan konfigurasi rilis publik.

Model utama berada di `model/`: `Wahana`, `Ticket`, `TicketBookingRequest`, serta model respons API. `AuthViewModel` menangani daftar/login/logout; `TourifyViewModel` menangani daftar wahana, riwayat tiket, dan pemesanan.

## Batasan implementasi yang perlu diketahui

- Username aktif disimpan sebagai state ViewModel selama proses aplikasi; belum ada token autentikasi atau penyimpanan sesi persisten yang terlihat.
- Riwayat tiket diminta berdasarkan username. Jika API/backend diubah, periksa implikasi privasi dan otorisasi pada kedua sisi.
- Base URL lokal menggunakan HTTP; manifest mengizinkan cleartext traffic dan akses jaringan lokal.
- Gambar destinasi dipilih dari resource lokal berdasarkan ID wahana di `HomeScreen.kt`; ID baru perlu dipetakan atau diberi fallback.
- Metode pembayaran pada layar pemesanan saat ini berupa pilihan UI (Bank, QRIS, E-Wallet); jangan menyimpulkan bahwa pembayaran diproses oleh penyedia pembayaran.

## Pengujian yang tersedia

- Unit test `TicketDateTest` memeriksa format tanggal yang ditampilkan pada tiket.
- Instrumentation test `TicketCardTest` memeriksa buka/tutup QR dan tampilan pada ukuran teks lebih besar.
- Ada juga file contoh tes standar Android/Compose.

Untuk panduan perubahan kode dan perintah build, lihat [`AGENTS.md`](AGENTS.md).
