# 🎬 ASCII Video Player v4 — Ultimate Edition

![Python](https://img.shields.io/badge/python-3.8+-blue.svg)
![OpenCV](https://img.shields.io/badge/OpenCV-4.x-green.svg)
![NumPy](https://img.shields.io/badge/NumPy-1.20+-blue.svg)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey.svg)

**ASCII Video Player v4** adalah pemutar video berbasis teks (Command-Line Interface) yang merender setiap *frame* video menjadi seni ASCII yang memukau langsung di dalam terminal Anda. Versi Ultimate ini mendukung *rendering* hitam-putih super cepat maupun *full-color* dengan performa pemrosesan *thread* terpisah!

---

## ✨ Fitur Utama

- **🌈 Mode Berwarna / Hitam Putih (`--color` / `--no-color`)**
  Pilih antara performa tinggi (Hitam-Putih) atau estetika visual (Berwarna).
- **🕹️ Interactive Mode & CLI Mode**
  Jalankan tanpa argumen untuk masuk ke mode interaktif yang ramah pengguna, atau gunakan langsung via *command-line* untuk integrasi otomatisasi.
- **⚡ Background Decoding Thread**
  Decoder video berjalan di *thread* terpisah secara *real-time* memastikan *playback* ASCII selancar mungkin tanpa *stuttering*.
- **📏 Kustomisasi Resolusi (`--width`)**
  Atur kelebaran karakter ASCII sesuka hati. Secara otomatis mendeteksi ukuran terminal Anda untuk *fallback*.
- **⏭️ Frame Skipping (`--skip`)**
  Menyediakan opsi untuk melompati *frame* jika terminal tidak sanggup me-*render* video dengan FPS tinggi.
- **🔄 Auto Loop (`--loop`)**
  Mainkan video secara berulang-ulang tiada henti.
- **ℹ️ Video Information (`--info`)**
  Intip informasi metadata dari file video Anda tanpa memutarnya.

---

## 🛠️ Persyaratan Sistem

Sebelum menjalankan program ini, pastikan Anda telah menginstal pustaka yang dibutuhkan:

```bash
pip install opencv-python numpy
```

---

## 🚀 Cara Penggunaan

### 1. Mode Interaktif (Sangat Mudah!)

Cukup jalankan file *script* tanpa argumen apa pun. Program akan memandu Anda langkah demi langkah:

```bash
python ASCII_v4_ultimate.py
```

### 2. Mode Command-Line (Cepat & Langsung)

Bagi pengguna *power user*, Anda bisa mengeksekusinya dalam satu baris perintah:

```bash
# Memutar video dengan warna (lebar otomatis menyesuaikan terminal)
python ASCII_v4_ultimate.py my_video.mp4 --color

# Memutar video hitam putih dengan lebar 150 karakter
python ASCII_v4_ultimate.py my_video.mp4 --width 150

# Memutar video berwarna, skip setiap 2 frame agar lebih ringan, dan loop terus menerus
python ASCII_v4_ultimate.py my_video.mp4 --color --skip 2 --loop
```

### Daftar Argumen Lengkap

| Argumen        | Tipe        | Deskripsi                                                                        |
| :------------- | :---------- | :------------------------------------------------------------------------------- |
| `video_path` | `String`  | Path file video yang ingin diputar.                                              |
| `--color`    | `Flag`    | Mengaktifkan rendering warna via*ANSI Escape Code*.                            |
| `--width`    | `Integer` | Menentukan jumlah kolom karakter ASCII (default: menyesuaikan lebar terminal).   |
| `--skip`     | `Integer` | Melompati beberapa frame (contoh:`--skip 2` untuk melompati tiap frame genap). |
| `--loop`     | `Flag`    | Memutar ulang video secara otomatis saat sudah selesai.                          |
| `--info`     | `Flag`    | Hanya menampilkan info resolusi, FPS, dan durasi video tanpa memutarnya.         |

---

## ⌨️ Kontrol

- Tekan `Ctrl + C` kapan saja untuk menghentikan (*Graceful Shutdown*) dan mengembalikan *cursor* terminal Anda seperti semula.

---

## 💡 Tips Performa

1. **Gunakan Terminal Modern:** Gunakan Windows Terminal, iTerm2, atau Alacritty untuk pengalaman rendering warna yang mulus. *Command Prompt (CMD)* klasik mungkin terasa lebih berat.
2. **Perkecil Font:** Jika gambar terlihat terpotong, perkecil ukuran *font* terminal Anda (`Ctrl` + `-` atau `Ctrl` + *Scroll Down*) agar lebih banyak karakter bisa ditampung.
3. **Hitam-Putih Lebih Cepat:** Rendering warna menambah beban komputasi. Matikan argumen `--color` jika PC mulai *lag* pada video beresolusi tinggi.
