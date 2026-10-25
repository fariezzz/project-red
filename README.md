# Project Red: Reels & Shorts Blocker (Anti-Doomscrolling)

Project Red adalah aplikasi Android native (Kotlin) yang dirancang untuk menghentikan kebiasaan doomscrolling secara cerdas, otomatis, dan hemat daya. Berbeda dengan aplikasi pemblokir konvensional yang sering memicu force-close atau loop tombol keluar, Project Red menggunakan mekanisme Smart Tab Remediation berbasis Android AccessibilityService yang memblokir konsumsi video pendek tanpa mengganggu fungsi komunikasi penting Anda.

Aplikasi ini beroperasi 100% lokal di perangkat Anda: tanpa pelacakan data, tanpa iklan, dan tanpa izin akses internet (`android.permission.INTERNET` ditiadakan sepenuhnya).

---

## Mengapa Memilih Project Red?

- **100% Privasi & Tanpa Akses Internet**: Berkas `AndroidManifest.xml` tidak memuat izin internet sama sekali. Seluruh evaluasi dan statistik berlangsung offline di perangkat. Data pribadi, pesan, atau riwayat penjelajahan tidak akan pernah keluar dari HP Anda.
- **Smart Remediation (Bebas Loop Keluar)**: Menekan tombol kembali pada tab Reels/Shorts sering kali menutup aplikasi dan mengulanginya saat dibuka kembali. Project Red mengatasi hal ini dengan mengalihkan tab secara cerdas ke Beranda atau Kotak Masuk (Inbox) tanpa menutup aplikasi.
- **Bebas False-Positive**: Fitur percakapan (Instagram DM, Facebook Messenger, TikTok Inbox), Cerita (Stories), penjelajahan linimasa profil teman, postingan feed grup, serta pemutaran video panjang reguler YouTube tetap berfungsi normal tanpa terganggu.
- **Metrik Waktu Terselamatkan (Habit Tracker)**: Menghitung secara otomatis berapa banyak video pendek yang berhasil dicegah setiap hari serta estimasi menit produktif yang berhasil Anda selamatkan dari manipulasi algoritma.
- **Kontrol Cepat Tanpa Buka Aplikasi**: Dilengkapi Notifikasi Status Interaktif (Ongoing Notification) dan Android Quick Settings Tile untuk mematikan, menyalakan, atau menjeda perlindungan langsung dari panel atas HP.
- **Sangat Ringan & Hemat Baterai**: Bukan pemrosesan citra (computer vision / OCR). Sistem hanya mengevaluasi pohon teks aksesibilitas dengan latensi di bawah 1 milidetik (< 0.001 detik).

---

## Cakupan Pemblokiran & Perlindungan

| Aplikasi | Package Identifier | Area yang Diblokir | Fitur yang Tetap Aman & Diizinkan |
| :--- | :--- | :--- | :--- |
| **Instagram** | `com.instagram.android` | Tab Reels aktif, penampil Reels layar penuh (Clips Viewer) | Direct Messages (DM), Cerita (Stories), Feed Beranda umum, Profil, Pencarian |
| **Facebook** | `com.facebook.katana`<br>`com.facebook.wakizashi`<br>`com.facebook.lite` | Tab Reels terpilih, penampil video pendek FB Shorts/Reels layar penuh | Messenger, kabar beranda (News Feed), halaman profil, detail postingan foto/album, postingan grup & halaman |
| **YouTube** | `com.google.android.youtube`<br>`com.google.android.youtube.tv` | Tab Shorts pada pivot bar, pemutar Shorts vertikal | Pemutaran video biasa (panjang) dengan seek bar, beranda, tab langganan (subscriptions) |
| **TikTok** | `com.zhiliaoapp.musically`<br>`com.ss.android.ugc.trill`<br>`com.zhiliaoapp.musically.go` | Feed video vertikal Beranda (Untuk Anda / Mengikuti) | Kotak Masuk (Inbox), ruang chat DM, halaman Profil, pencarian, dan tab Teman |

---

## Fitur Utama

### 1. Pelacak Waktu & Statistik Harian
- Menghitung total pemblokiran harian dan sepanjang masa secara lokal.
- Estimasi waktu hidup yang terselamatkan (dihitung berbasis rata-rata 3 menit per sesi video pendek yang dicegah).
- Rincian penghitung individual untuk masing-masing platform (Instagram, Facebook, YouTube, TikTok).

### 2. Quick Snooze (Jeda Sementara)
- Opsi kelonggaran sementara (5 menit, 15 menit, atau 30 menit) saat Anda perlu menonton tautan video tertentu dari teman.
- Timer countdown otomatis dengan fitur auto-resume: perlindungan langsung aktif kembali setelah durasi jeda berakhir tanpa takut lupa menyalakan ulang.

### 3. Notifikasi Status Permanen (Ongoing Notification)
- Tersemat secara senyap di panel notifikasi Android tanpa suara berulang.
- Menampilkan status perlindungan dan ringkasan statistik harian secara real-time.
- Memuat tombol aksi cepat: `[Jeda 15 Mnt]`, `[Matikan]`, atau `[Nyalakan]` / `[Lanjutkan]` langsung dari layar kunci atau status bar.
- Fitur auto-reappear: jika notifikasi tergeser atau terhapus secara tidak sengaja, sistem akan memunculkannya kembali otomatis.

### 4. Android Quick Settings Tile
- Tile resmi sistem Android yang dapat disematkan di sebelah tombol Wi-Fi/Senter pada panel pengaturan cepat HP.
- Mengubah status proteksi atau membatalkan jeda dengan satu sentuhan jari.

### 5. Jadwal Mode Fokus & Jam Tidur (Focus Hours)
- Pembatasan pemblokiran otomatis pada rentang jam rawan doomscrolling.
- Dilengkapi dua prasetel praktis:
  - Prasetel Jam Tidur: Otomatis aktif pukul 22.00 - 06.00 WIB untuk mencegah insomnia akibat layar ponsel.
  - Prasetel Jam Kerja: Otomatis aktif pukul 08.00 - 17.00 WIB untuk menjaga konsentrasi belajar atau bekerja.

### 6. Umpan Balik Getaran Halus (Haptic Feedback)
- Getaran fisik lembut (*haptic click*) saat aksi pemblokiran atau pengalihan tab terjadi, melatih kesadaran bawah sadar terhadap kebiasaan reflek membuka video pendek.

---

## Arsitektur Teknis

- **Abstraksi Pohon UI (`UiNodeSnapshot`)**: Seluruh logika evaluasi deteksi dipisahkan dari dependensi native Android AccessibilityNodeInfo ke model Kotlin murni, memungkinkan pengujian unit level JVM berjalan dalam hitungan milidetik tanpa emulator.
- **Penyaringan Node Tak Terlihat (`isVisibleToUser`)**: Subtree node yang tersimpan dalam cache FragmentManager atau ViewPager di latar belakang secara otomatis dipangkas (*pruned*) sehingga tidak menimbulkan false-positive.
- **Pelindung Siklus Macet (Stuck-Loop Fallback)**: Jika terjadi anomali pemblokiran berulang pada layar modal yang sama (>= 4 kali dalam 5 detik), sistem secara aman mengalihkan pengguna ke Beranda perangkat (`GLOBAL_ACTION_HOME`).

---

## Verifikasi & Pengujian Unit

Proyek ini dilengkapi dengan 59 rangkaian pengujian unit murni yang mencakup seluruh skenario deteksi, batas waktu jeda, perhitungan statistik, jadwal malam, dan pencegahan false-positive:

```bash
# Menjalankan pengujian unit
./gradlew testDebugUnitTest --no-daemon
```

Laporan hasil pengujian:
`app/build/reports/tests/testDebugUnitTest/index.html`

Membangun file APK:
```bash
# Membangun file APK Debug
./gradlew assembleDebug --no-daemon
```

Lokasi berkas APK hasil kompilasi:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Panduan Instalasi & Penggunaan

### 1. Pemasangan Berkas APK
Hubungkan ponsel Android Anda dengan USB Debugging aktif, lalu jalankan:
```bash
# Memasang aplikasi ke perangkat Android
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Menjalankan aplikasi
adb shell am start -n com.projectred.scrollguard.debug/com.projectred.scrollguard.ui.MainActivity
```

### 2. Mengaktifkan Layanan Aksesibilitas
1. Buka aplikasi **Project Red** di perangkat Anda.
2. Ketuk tombol **Buka Pengaturan Aksesibilitas** (atau buka menu *Pengaturan HP > Aksesibilitas > Aplikasi Terinstal*).
3. Pilih **Project Red** dan aktifkan sakelar izin.
4. Kembali ke aplikasi Project Red. Banner status akan berubah menjadi hijau (**Perlindungan Aktif**).
5. Atur sakelar aplikasi target dan preferensi fitur tambahan sesuai kebutuhan Anda.

---

## Lisensi

Proyek ini dirilis di bawah lisensi MIT. Bebas digunakan, dipelajari, dan dikembangkan lebih lanjut untuk tujuan produktivitas dan kesejahteraan digital.
