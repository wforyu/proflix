# ProFlix

Aplikasi streaming Android native modern dengan pengalaman seperti Netflix. Fokus pada performa, privasi, dan arsitektur modular.

## Features

### Core
- **Multi-Provider Architecture** - Support berbagai sumber anime (Anoboy, Samehadaku, Oploverz)
- **Provider Switching** - Ganti provider kapan saja via Settings
- **Custom Domain** - Setting domain sendiri untuk setiap provider
- **Privacy-First** - Tanpa analytics, tracking, atau iklan

### Player
- **ExoPlayer (Media3)** - Untuk direct video URLs (MP4, HLS, DASH)
- **WebView Player** - Untuk iframe-based streaming
- **Auto-Play Next** - Otomatis putar episode berikutnya
- **Episode Navigation** - Navigasi episode sebelumnya/selanjutnya dari player
- **Source Selection** - Pilih mirror/quality yang tersedia

### UI
- **Material 3 Design** - Desain modern sesuai Material You
- **Dark Theme** - Tema gelap default
- **Jetpack Compose** - UI declarative modern
- **Bottom Navigation** - Navigasi cepat antar halaman

### Content
- **Home** - Trending, Latest, dan kategori konten
- **Search** - Pencarian instan anime
- **Detail** - Info lengkap anime + daftar episode
- **History** - Riwayat tontonan
- **Favorites** - Anime favorit

## Tech Stack

| Komponen | Versi |
|---|---|
| Kotlin | 2.0.0 |
| Gradle | 8.7 |
| AGP | 8.5.0 |
| Compose BOM | 2024.06.00 |
| Media3 ExoPlayer | 1.3.1 |
| Hilt | 2.51.1 |
| Room | 2.6.1 |
| Navigation Compose | 2.7.7 |
| Jsoup | 1.18.1 |
| Retrofit | 2.11.0 |
| Coil | 2.7.0 |

## Architecture

```
MVVM + Clean Architecture

UI (Compose) -> ViewModel -> UseCase -> Repository -> DataSource -> API / Database
```

### Module Structure

```
app/                    -> Main application
core/
  common/               -> Utilities, extensions
  network/              -> Retrofit, OkHttp
  database/             -> Room, DataStore
  designsystem/         -> Theme, Compose components
  player/               -> ExoPlayer, WebView player
feature/
  home/                 -> Home screen
  search/               -> Search screen
  detail/               -> Anime detail
  player/               -> Stream player
  history/              -> Watch history
  favorite/             -> Favorites
  settings/             -> App settings
provider/
  domain/               -> Provider interface, models
  data/                 -> Provider implementations (scrapers)
```

## Providers

| Provider | URL | Metode |
|---|---|---|
| Anoboy | anoboy.pk | Jsoup HTML parsing |
| Samehadaku | v2.samehadaku.how | Jsoup + AJAX |
| Oploverz | oploverz.site | SvelteKit SSR |

## Build

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# Clean
./gradlew clean assembleDebug
```

## Requirements

- Android 10+ (API 29)
- Target SDK: 34 (Android 14)
- JVM Target: 17

## Privacy

ProFlix TIDAK menggunakan:
- Analytics SDK
- Advertising SDK
- Tracking SDK
- Firebase Analytics
- Third party trackers

## License

Private - All Rights Reserved
