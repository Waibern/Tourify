# Panduan kerja agen — Tourify

## Ringkasan proyek

Tourify adalah aplikasi Android berbahasa Indonesia untuk melihat destinasi/wahana, memesan tiket, dan melihat tiket yang sudah dipesan. Aplikasi memakai Kotlin, Jetpack Compose, Navigation Compose, Retrofit, dan ViewModel.

## Cara bekerja

- Periksa status Git sebelum mulai. Hormati perubahan yang sudah ada; jangan menimpa atau mengembalikannya tanpa diminta.
- Baca file pemilik fitur dan alur pemanggilnya sebelum mengubah kode. `MainActivity.kt` mengatur navigasi dan dependensi layar; periksa alurnya sebelum menambah rute atau state global.
- Buat perubahan sesempit mungkin dan ikuti pola proyek. Jangan menganggap ada lapisan repository atau sesi persisten jika belum terlihat di kode.
- Untuk kontrak API, cocokkan model Kotlin, `ApiService`, pemanggil di ViewModel, dan kontrak backend PHP yang tersedia. Jangan menebak nama field atau format tanggal.
- Pertahankan teks antarmuka dalam bahasa Indonesia dan gaya yang sudah ada: tata letak jelas, mudah dibaca, warna hijau-putih, dan dekorasi secukupnya.
- Jaga aksesibilitas Compose: label konten yang bermakna, dukungan ukuran teks besar, area klik yang wajar, serta state loading/error/kosong jika relevan.
- Jangan menaruh kredensial, token, atau konfigurasi mesin pribadi ke source maupun dokumentasi. `local.properties` adalah konfigurasi lokal; jangan menyalin isinya ke commit.

## Gaya kode dan komentar

- Semua file Kotlin di `screen/`, termasuk komponen pendukung dan preview, wajib memakai susunan ke bawah seperti contoh di bawah. Gunakan indentasi 4 spasi untuk setiap tingkat blok.
- Kalau pemanggilan fungsi atau komponen punya lebih dari satu argumen, tulis setiap argumen di baris sendiri. Letakkan `(` setelah nama fungsi, isi argumen di bawahnya, lalu `)` di baris penutup; untuk komponen yang punya isi, lanjutkan dengan `) {`.
- Rantai `Modifier` yang punya beberapa operasi harus ditulis ke bawah: mulai dari `Modifier`, lalu satu operasi `.fillMaxSize()`, `.background(...)`, `.padding(...)`, dan seterusnya per baris dengan tambahan satu tingkat indentasi.
- Isi lambda serta blok berkurung kurawal pada `onClick`, `onValueChange`, `remember`, `LaunchedEffect`, maupun kondisi harus ditulis ke bawah, bukan disisipkan bersama isinya di satu baris. Tulis kondisi panjang ke beberapa baris dengan indentasi yang jelas. Jangan menggabungkan beberapa perintah memakai `;`. Lambda kosong `{}` dan pemanggilan sederhana dengan satu argumen, seperti `Spacer(modifier = Modifier.height(25.dp))`, boleh tetap ringkas.
- Beri komentar pada setiap bagian kode yang ditambah atau diubah, tepat di dekat bagian yang dijelaskan. Pakai bahasa Indonesia yang santai, singkat, dan jelas, misalnya `// notif berhasil`, `// ambil daftar tiket`, atau `// tampilkan pesan kalau gagal`.
- Komentar harus menjelaskan maksud atau alur bagian kode tersebut dan tetap sesuai perilaku terbaru. Hindari komentar berulang pada tiap baris sederhana, komentar yang cuma mengulang nama fungsi, dan komentar yang sudah tidak benar.
- Ikuti sintaks komentar sesuai jenis file (`//` untuk Kotlin, `<!-- ... -->` untuk XML). Jangan menaruh data pribadi atau rahasia di komentar.

Contoh format Compose yang harus diikuti:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
        .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    // judul di atas isi layar
    Text(
        text = "Username",
        color = Color.Gray,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}
```

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

