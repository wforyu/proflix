# AGENTS.md

# Project: ProFlix

## Visi

ProFlix adalah aplikasi streaming Android native modern dengan pengalaman seperti Netflix.

Tujuan:

- Cepat.
- Ringan.
- Fokus privasi.
- Performa native Android.
- Arsitektur backend/provider modular.
- Tanpa sistem akun wajib.
- Tanpa tracking yang tidak perlu.
- Pengalaman video playback yang lancar.

ProFlix harus terasa seperti aplikasi streaming premium.

---

# Prinsip Inti

## Filosofi Pengembangan

Ikuti:

- Clean Architecture.
- MVVM.
- Prinsip SOLID.
- Separation of concerns.
- Pengembangan modular berbasis fitur.
- Kode yang dapat diuji.
- Arsitektur yang mudah dirawat.

Hindari:

- Kode spaghetti.
- Activity class yang terlalu besar.
- Business logic di dalam UI.
- Data hardcoded.
- Kode duplikat.

---

# Platform

Target:

- Android 10+.
- Kotlin only.
- Jetpack Compose UI.
- Material 3 design.

Minimum SDK: Android 10 (API 29)
Target SDK: 34 (Android 14)
Compile SDK: 34

---

# Stack Teknologi & Versi

## Build System

| Komponen | Versi |
|---|---|
| Gradle | 8.7 |
| Android Gradle Plugin (AGP) | 8.5.0 |
| Kotlin | 2.0.0 |
| KSP (Kotlin Symbol Processing) | 2.0.0-1.0.21 |
| JVM Target | 17 |
| Java Source/Target | 17 |

## Build Command

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Lint check
./gradlew lintDebug
```

## Versi Aplikasi

| Property | Value |
|---|---|
| applicationId | com.proflix.app |
| versionCode | 1 |
| versionName | 1.0.0 |
| minSdk | 29 (Android 10) |
| targetSdk | 34 (Android 14) |

---

# Library & Dependensi

## Version Catalog (`gradle/libs.versions.toml`)

### Core

| Library | Versi | Fungsi |
|---|---|---|
| Kotlin | 2.0.0 | Bahasa pemrograman utama |
| Kotlinx Coroutines | 1.8.1 | Async programming |
| Kotlinx Serialization | 1.7.0 | JSON serialization |

### Android

| Library | Versi | Fungsi |
|---|---|---|
| AndroidX Core KTX | 1.13.1 | Extension Kotlin untuk Android |
| Lifecycle Runtime KTX | 2.8.2 | Lifecycle-aware components |
| Lifecycle Runtime Compose | 2.8.2 | Compose lifecycle integration |
| Lifecycle ViewModel Compose | 2.8.2 | ViewModel untuk Compose |
| Activity Compose | 1.9.0 | Activity integration Compose |
| DataStore Preferences | 1.1.1 | Local key-value storage |

### UI

| Library | Versi | Fungsi |
|---|---|---|
| Compose BOM | 2024.06.00 | Bill of Materials Compose |
| Compose UI | (via BOM) | Core UI toolkit |
| Compose UI Graphics | (via BOM) | Graphics rendering |
| Compose UI Tooling | (via BOM) | Debug tools |
| Compose UI Tooling Preview | (via BOM) | Preview support |
| Material 3 | (via BOM) | Material Design 3 |
| Material Icons Extended | (via BOM) | Ikon Material Design |
| Compose Foundation | (via BOM) | Foundation composables |
| Compose Animation | (via BOM) | Animation support |
| Navigation Compose | 2.7.7 | Navigation Jetpack Compose |

### Networking

| Library | Versi | Fungsi |
|---|---|---|
| Retrofit | 2.11.0 | HTTP client |
| Retrofit KotlinX Serialization | 2.11.0 | Converter untuk serialization |
| OkHttp | 4.12.0 | HTTP client底层 |
| OkHttp Logging Interceptor | 4.12.0 | Request/response logging |
| Jsoup | 1.18.1 | HTML parser (web scraping) |

### Dependency Injection

| Library | Versi | Fungsi |
|---|---|---|
| Hilt Android | 2.51.1 | DI framework |
| Hilt Compiler | 2.51.1 | Annotation processor |
| Hilt Navigation Compose | 1.2.0 | Hilt + Navigation Compose |

### Database

| Library | Versi | Fungsi |
|---|---|---|
| Room Runtime | 2.6.1 | Local database |
| Room KTX | 2.6.1 | Coroutines support |
| Room Compiler | 2.6.1 | Annotation processor |

### Media / Player

| Library | Versi | Fungsi |
|---|---|---|
| Media3 ExoPlayer | 1.3.1 | Video player engine |
| Media3 UI | 1.3.1 | Player UI components |
| Media3 Session | 1.3.1 | Media session management |
| Media3 Common | 1.3.1 | Shared utilities |
| Media3 ExoPlayer HLS | 1.3.1 | HLS streaming support |
| Media3 ExoPlayer DASH | 1.3.1 | DASH streaming support |

### Image Loading

| Library | Versi | Fungsi |
|---|---|---|
| Coil Compose | 2.7.0 | Image loading untuk Compose |

### Testing

| Library | Versi | Fungsi |
|---|---|---|
| JUnit | 4.13.2 | Unit testing framework |
| AndroidX JUnit | 1.2.1 | Android testing extension |
| Espresso Core | 3.6.1 | UI testing |
| Compose UI Test Manifest | (via BOM) | Compose testing |
| Compose UI Test JUnit4 | (via BOM) | Compose testing rules |

---

# Arsitektur

Gunakan: **MVVM + Clean Architecture**

Alur:

```
UI (Compose)
  |
ViewModel
  |
UseCase
  |
Repository
  |
DataSource
  |
API / Database
```

---

# Dependency Injection

Gunakan: **Hilt**

Semua dependency harus di-inject.

Hindari:
- Manual singleton.
- Global objects.
- Static helpers.

---

# Struktur Proyek (Multi-Module)

```
app/                          -> Aplikasi utama
core/
  common/                     -> Utility, extension, base classes
  network/                    -> Retrofit, OkHttp, network config
  database/                   -> Room, DataStore
  designsystem/               -> Theme, Compose components
  player/                     -> ExoPlayer, WebView player
feature/
  home/                       -> Beranda, banner, trending
  search/                     -> Pencarian anime
  detail/                     -> Detail anime, daftar episode
  player/                     -> Stream player, source selection
  history/                    -> Riwayat tontonan
  favorite/                   -> Anime favorit
  settings/                   -> Pengaturan, switch provider
provider/
  domain/                     -> Provider interface, model data
  data/                       -> Implementasi provider (scraper)
  ui/                         -> Provider-related UI (empty)
```

**Total modul: 15**

---

# Arsitektur Provider

Sistem provider memungkinkan berbagai sumber backend berbeda.

Aplikasi tidak boleh bergantung pada satu backend saja.

### Provider yang Tersedia

| Provider | Tipe | Metode Scraping | URL Default |
|---|---|---|---|
| Anoboy | Anime Indonesia | Jsoup HTML parsing | https://anoboy.pk |
| Samehadaku | Anime Indonesia | SOKUJA Next.js RSC + API | https://x6.sokuja.uk |
| Oploverz | Anime Indonesia | SvelteKit SSR JSON extraction | https://oploverz.site |

### Interface Provider

```kotlin
interface Provider {
    suspend fun getHome(): Result<HomeContent>
    suspend fun getTrending(): Result<List<Content>>
    suspend fun getLatest(): Result<List<Content>>
    suspend fun getDetail(id: String): Result<Content>
    suspend fun getEpisodes(contentId: String): Result<List<Episode>>
    suspend fun getStream(episodeId: String): Result<List<StreamSource>>
    suspend fun search(query: String): Result<List<Content>>
    suspend fun getProviderName(): String
}
```

### Model Data

- `Content` -> Data anime (id, title, poster, banner, description, genres, year, rating, type)
- `Episode` -> Data episode (id, contentId, season, number, title, description, thumbnail, duration)
- `StreamSource` -> Sumber streaming (url, quality, format)
- `HomeContent` -> Konten halaman beranda (hero, trending, latest, continueWatching, categories)
- `ContentType` -> Enum (ANIME, MOVIE, SERIES)

### ProviderManager

- Switching provider aktif via `ProviderType` enum
- Custom domain untuk setiap provider via `setCustomDomain()`
- Default domain per provider via `getDefaultDomain()`
- Default provider: Anoboy

---

# Detail Scraping per Provider

## Anoboy

- **URL**: `anoboy.pk` (sebelumnya `anoboy.unogs.com`)
- **Tipe**: WordPress + AnimeStream Theme
- **Home**: Selector `.serieslist.pop.wpop ul li` untuk trending, `article.bs > .bsx` untuk latest
- **Detail**: Selector `.spe`, `.spe h2`, `.infox .fzps`
- **Episodes**: Selector `.eplister ul li`, `.episodelist ul li`, `.listeps li`
- **Streams**: Mirror diambil dari `select.mirror option[value=base64]`, lalu di-decode base64. Fallback ke `#pembed iframe` dan `.soraddlx .soraurlx`. **Embed URLs di-resolve via `EmbedVideoExtractor` ke direct MP4**
- **Search**: `article.bs` dengan selector `.tt h2[itemprop="headline"]`

## Samehadaku

- **URL**: `x6.sokuja.uk` (sebelumnya `v2.samehadaku.how`)
- **Tipe**: Next.js SPA (SOKUJA) dengan React Server Components
- **Home**: Sidebar "Anime Populer" dari static HTML (`aside ul li a[href*="/anime/"]`). Hidden `<div id="S:0">` berisi hero carousel. Main content di-load via RSC streaming
- **Detail**: LD+JSON structured data (`TVSeries` schema) dengan `name`, `description`, `genre[]`, `aggregateRating`. Episode grid dari HTML
- **Episodes**: Parse `<a href*="-episode-" href*="-subtitle-indonesia">` dari halaman detail. Episode URL pattern: `/{slug}-episode-{N}-subtitle-indonesia/`
- **Streams**: API endpoint `/api/video-mirrors?e={episodeId}` → direct MP4 URLs. `episodeId` numerik di-extract dari RSC data (`"episodeId":{number}`)
- **Search**: Query ke `/?s={query}`, parsing result links dari static HTML

## Oploverz

- **URL**: `oploverz.site` (fallback: `backapi.oploverz.ac`)
- **Tipe**: SvelteKit SPA dengan SSR data
- **Home**: Data di-extract dari embedded JSON di dalam `<script>` tag menggunakan SvelteKit data extraction
- **SvelteKit Parsing**: Mencari `node_ids:` -> `data:` -> balanced bracket extraction -> merge `JSONArray` items dengan `type: "data"`
- **Fallback**: `const/let/var data = {...}` pattern, lalu Jsoup untuk iframe
- **Detail**: Extract dari `pageData.series` atau `pageData.anime`
- **Episode List**: Primary method: parse `<a href="/series/{slug}/episode/{N}">` dari static HTML. Fallback: extract dari `pageData.episodes` array (SvelteKit data). Handle anime 1000+ episode (HTML parsing lebih ringan dari SvelteKit data ~4MB)
- **Streams**: Extract dari `pageData.episode.streamUrl` array. Fallback ke `pageData.streamUrl` array, lalu Jsoup iframe parsing
- **Search**: Query ke `/series?q=$query`, extract dari `pageData.series.data` atau `pageData.search.data`

---

# Player

## Tipe Player

### ExoPlayer (Media3)
- Satu-satunya player yang digunakan
- Untuk direct video URLs (MP4, HLS, DASH)
- Mendukung: play, pause, seek, resume, playback speed, fullscreen, subtitle
- Embed URLs di-resolve ke direct URLs oleh `EmbedVideoExtractor` sebelum dikirim ke ExoPlayer

## EmbedVideoExtractor

- Lokasi: `core/network/EmbedVideoExtractor.kt`
- Fungsi: Scrape embed pages untuk extract direct video URLs
- Metode: 6 regex patterns + video host fallback
- Known hosts: turboviplay.com, etvp.cc, turboplay.cc, embedwish.com, sumpiernos.com, vcdn2.mystream.to
- Return: `VideoResult(url, format)` atau `null`
- Helper: `isDirectVideoUrl()`, `detectFormat()`

## Alur Stream Resolution

```
User klik "Putar Episode"
  |
StreamPlayerViewModel.resolveStream(episodeId, title, contentId)
  |
ProviderRepository.getStream(episodeId) -> List<StreamSource>
  |
Provider getStream() -> If embed URL, resolve via EmbedVideoExtractor -> direct MP4 URL
  |
Load episodes (jika contentId tersedia) -> Auto-play support
  |
ExoPlayer (semua URL sudah di-resolve ke direct MP4/HLS/DASH)
```

## Fitur Player

### Playback
- Play, Pause, Seek, Resume
- Playback speed
- Fullscreen

### Auto-Play & Episode Navigation
- `playNextEpisode()` / `playPreviousEpisode()` -> Navigate between episodes
- `onPlaybackEnded()` -> Auto-play next episode jika enabled
- `toggleAutoPlayNext()` -> Toggle auto-play on/off
- `hasNextEpisode()` / `hasPreviousEpisode()` -> Check episode bounds

### UI
- Source selector (pilih kualitas/mirror)
- Back button
- Loading indicator
- "No stream available" placeholder
- Prev/Next episode buttons
- Auto-play toggle switch

---

# Data Storage

## Room Database

Data lokal:
- **History**: anime, episode, position, duration, last watched
- **Continue Watching**: content_id, episode_id, watch_position, progress
- **Favorite**: anime_id, title, poster, added_date

## DataStore Preferences

Menyimpan:
- Provider aktif (Anoboy / Samehadaku / Oploverz)
- Custom domain per provider
- Tema (gelap/terang)
- Pengaturan player
- Kualitas preferensi
- Pengaturan subtitle

---

# Navigasi

Gunakan: **Navigation Compose**

Destinasi utama:

| Screen | Route | Fungsi |
|---|---|---|
| Home | `home` | Beranda dengan konten trending/latest |
| Search | `search` | Pencarian anime |
| Detail | `detail/{contentId}` | Detail anime + daftar episode |
| Player | `player?episodeId={episodeId}&title={title}&contentId={contentId}` | Stream player |
| History | `history` | Riwayat tontonan |
| Favorite | `favorite` | Anime favorit |
| Settings | `settings` | Pengaturan provider & aplikasi |

---

# Error & Bug Fix yang Pernah Terjadi

## 1. Navigation Compose Crash karena Content ID

**Masalah**: Content ID mengandung karakter `/` (contoh: `anime/xyz`) yang memecah route Navigation Compose, menyebabkan crash.

**Error**: `IllegalArgumentException: Malformed route`

**Fix**: 
- `Screen.Detail.createRoute()` sekarang menggunakan `URLEncoder.encode(contentId, "UTF-8")`
- `DetailViewModel` menggunakan `URLDecoder.decode(encodedContentId, "UTF-8")`

**File**: `app/navigation/Screen.kt`, `feature/detail/DetailViewModel.kt`

## 2. ProviderRepository Menggunakan Retrofit yang Salah

**Masalah**: `ProviderRepositoryImpl` menggunakan `ProviderApi` (Retrofit interface) alih-alih `ProviderManager`, sehingga tidak bisa mengakses provider yang benar.

**Error**: Semua provider return data kosong/gagal

**Fix**: 
- `ProviderRepositoryImpl` sekarang menggunakan `ProviderManager.getCurrentProvider()` untuk mendapatkan provider aktif
- `ProviderApiModule` dihapus dari `ProviderModule.kt`

**File**: `provider/data/ProviderRepositoryImpl.kt`, `provider/data/ProviderModule.kt`

## 3. Crash saat Klik Konten

**Masalah**: `episodeId` kosong saat dikirim ke player, menyebabkan crash.

**Error**: NullPointerException / Empty stream URL

**Fix**:
- `ProFlixNavHost.kt` `onPlayEpisode` sekarang memanggil `navController.navigateToPlayer(episodeId, title)` alih-alih hardcode URL kosong
- Route player baru: `player?episodeId={episodeId}&title={title}`

**File**: `app/navigation/ProFlixNavHost.kt`, `feature/player/PlayerNavigation.kt`

## 4. Nullable Type Error di AnoboyProvider

**Masalah**: `?.text()` menghasilkan `String?`, lalu dipanggil `.ifBlank {}` tanpa safe call.

**Error**: `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'kotlin.String?'`

**Fix**:
```kotlin
// Sebelum (error)
val epTitle = ep.selectFirst("h3")?.text().ifBlank { ... }

// Sesudah (benar)
val epTitle = ep.selectFirst("h3")?.text()?.ifBlank { ... } ?: "fallback"
```

**File**: `provider/data/AnoboyProvider.kt:119`

## 5. Weight Modifier di Luar ColumnScope

**Masalah**: `Modifier.weight(1f)` digunakan di dalam `WebViewContent()` composable yang berdiri sendiri, bukan di dalam `ColumnScope`.

**Error**: `Unresolved reference 'weight'`

**Fix**: Ganti `Modifier.fillMaxWidth().weight(1f)` dengan `Modifier.fillMaxSize()` di dalam `WebViewContent`.

**File**: `feature/player/StreamPlayerScreen.kt:210`

## 6. Player Navigation tanpa ContentId

**Masalah**: Player route tidak menyertakan `contentId`, sehingga auto-play next/previous episode tidak berfungsi karena episode list tidak bisa diload.

**Fix**:
- Route player diperbarui: `player?episodeId={episodeId}&title={title}&contentId={contentId}`
- `ProFlixNavHost.kt` `onPlayEpisode` sekarang meneruskan `contentId`
- `StreamPlayerViewModel.resolveStream()` menerima `contentId` dan memanggil `loadEpisodes()` jika tersedia

**File**: `feature/player/PlayerNavigation.kt`, `app/navigation/ProFlixNavHost.kt`, `feature/player/StreamPlayerViewModel.kt`

## 7. SamehadakuProvider Migration ke SOKUJA

**Masalah**: Samehadaku lama (`v2.samehadaku.how`) sudah tidak berfungsi. Migrasi ke SOKUJA (`x6.sokuja.uk`) yang menggunakan Next.js RSC.

**Fix**:
- Rewrite lengkap `SamehadakuProvider.kt` menggunakan arsitektur SOKUJA
- Sidebar "Anime Populer" di-parse dari static HTML (`aside ul li a[href*="/anime/"]`)
- Detail anime dari LD+JSON structured data (`TVSeries` schema)
- Episodes di-parse dari daftar episode di halaman detail (`a[href*="-episode-"]`)
- Streams menggunakan API endpoint SOKUJA: `/api/video-mirrors?e={episodeId}` → direct MP4 URLs
- Search menggunakan `/?s={query}` dengan parsing HTML
- `ProviderManager.kt` default domain di-update ke `https://x6.sokuja.uk`

**File**: `provider/data/SamehadakuProvider.kt`, `provider/data/ProviderManager.kt`

## 8. Oploverz 1000+ Episode Tidak Muncul

**Masalah**: Anime dengan episode 1000+ (contoh: One Piece 1169 episode) tidak menampilkan episode di SvelteKit page data karena data terlalu besar (~4MB) dan parsing `org.json` gagal/OOM di Android.

**Fix**:
- `getEpisodes()` sekarang menggunakan HTML parsing sebagai primary method
- Parse `<a href="/series/{slug}/episode/{N}">` links dari static HTML (ringan, semua episode termasuk)
- SvelteKit data extraction tetap sebagai fallback
- Null titles di-handle dengan fallback `"Episode $epNum"`

**File**: `provider/data/OploverzProvider.kt`

## 9. WebView Player Diganti ExoPlayer dengan EmbedVideoExtractor

**Masalah**: Player menggunakan WebView untuk iframe-based streaming yang tidak stabil, lambat, dan tidak konsisten di beberapa device.

**Fix**:
- Buat `EmbedVideoExtractor` di `core/network/` untuk resolve embed URLs ke direct MP4
- 6 regex patterns untuk extract video URLs dari embed pages
- Known hosts: turboviplay.com, etvp.cc, sumpiernos.com, vcdn2.mystream.to
- `isDirectVideoUrl()` dan `detectFormat()` helpers
- `AnoboyProvider.getStream()` resolve embed URLs via `EmbedVideoExtractor`
- `OploverzProvider.getStream()` resolve embed URLs via `EmbedVideoExtractor`
- `SamehadakuProvider.getStream()` fallback resolve via `EmbedVideoExtractor`
- `StreamSource.format` field diberikan default value `"mp4"`
- Hapus `WebViewContent` composable dari `StreamPlayerScreen.kt`
- Hapus semua WebView imports dan logic
- `StreamPlayerScreen` sekarang hanya menggunakan ExoPlayer

**File**: `core/network/EmbedVideoExtractor.kt`, `provider/data/AnoboyProvider.kt`, `provider/data/OploverzProvider.kt`, `provider/data/SamehadakuProvider.kt`, `provider/domain/model/StreamSource.kt`, `feature/player/StreamPlayerScreen.kt`

## 10. EmbedVideoExtractor Gagal Resolve Semua Embed URLs

**Masalah**: `EmbedVideoExtractor` gagal resolve URL dari semua 3 provider — Anoboy, Samehadaku, dan Oploverz semuanya return "no stream available". Regex patterns tidak cocok dengan modern embed pages.

**Root Cause (Deep Investigation)**:
- **Anoboy**: 2/3 episodes menggunakan **Blogger video tokens** (`blogger.com/video.g?token=...`); 1/3 menggunakan `turbovidhls.com`. Blogger embed pages adalah SPA — tidak ada direct video URLs di initial HTML
- **Oploverz**: 96% `streamUrl` entries menuju `blogger.com/video.g?token=...`; sisanya `filedon.co/embed`, `upbolt.to/e/`, `4meplayer.pro`, `abyssplayer.com`
- **Samehadaku**: API `/api/video-mirrors?e={numericEpisodeId}` berfungsi dan return direct MP4 dari `storages.sokuja.uk` — masalah di `extractEpisodeId()` regex yang gagal karena escaped quotes di Next.js RSC payload

**Fix**:
- Rewrite `EmbedVideoExtractor` dengan Blogger batchexecute RPC API (`/_/BloggerVideoPlayerUi/data/batchexecute`)
- Token di-extract dari URL via `token=([^&"'\s]+)` regex
- Response di-parse untuk extract `googlevideo.com/videoplayback` URLs dengan unescape `\\u003d` → `=`, `\\u0026` → `&`
- `isBloggerUrl()` check: `blogger.com/video.g` atau `blogger.com/video?`
- Added known hosts: `turboviplay.com`, `etvp.cc`, `sptvp.com`, `turbovidhls.com`, `embedwish.com`, `sumpiernos.com`, `vcdn2.mystream.to`, `filedon.co`, `upbolt.to`, `4meplayer.com`, `4meplayer.pro`, `abyssplayer.com`
- `fetchVideoMirrors()` di Samehadaku tetap fallback ke iframe parsing jika API gagal

**File**: `core/network/EmbedVideoExtractor.kt`, `provider/data/SamehadakuProvider.kt`

## 11. Samehadaku extractEpisodeId() Regex Gagal di RSC Payload

**Masalah**: `extractEpisodeId()` tidak menemukan numeric ID di Next.js RSC payload karena escaped quotes (`\"episodeId\"`) dalam `<script>self.__next_f.push()</script>` tags.

**Fix**:
- Tambah regex patterns untuk handle escaped quotes: `["\\]*\s*[:=]\s*["\\]*(\d+)` dan `\u0022episodeId\u0022\s*:\s*(\d+)`
- Simplified `getStream()` — hapus unnecessary LD+JSON check yang memanggil method tidak ada

**File**: `provider/data/SamehadakuProvider.kt`

## 12. SamehadakuStream Resolution Fix - LD+JSON Episode Number

**Masalah**: `extractEpisodeId()` tetap gagal karena RSC payload menggunakan escaped quotes. `fetchVideoMirrors()` tidak pernah dipanggil karena numeric ID tidak ditemukan.

**Root Cause**: Episode page SOKUJA memiliki LD+JSON `TVEpisode` schema dengan `episodeNumber` field yang bisa diandalkan. RSC payload regex tidak stabil karena escaped quotes yang bervariasi.

**Fix**:
- Ganti `extractEpisodeId()` dengan `extractEpisodeNumberFromLdJson()` yang mencari `"episodeNumber":\s*(\d+)` di seluruh HTML
- LD+JSON dijamin ada di episode page SOKUJA (terbukti dari investigasi: `"episodeNumber":1170`)
- `getStream()` sekarang memanggil `fetchVideoMirrors()` dengan episode number yang benar
- Fallback ke iframe parsing jika LD+JSON tidak ditemukan

**File**: `provider/data/SamehadakuProvider.kt`

## 13. EmbedVideoExtractor Blogger Resolution Fix

**Masalah**: Batchexecute RPC API tidak menghasilkan googlevideo URLs. Respon parser tidak menemukan video URLs di response.

**Root Cause**: Blogger embed pages (`blogger.com/video.g?token=...`) adalah SPA yang memuat video player secara dinamis. Video URLs bisa ditemukan di HTML response embed page itu sendiri.

**Fix**:
- Hapus batchexecute RPC approach yang kompleks
- Fetch Blogger embed page langsung dan parse video URLs dari HTML/JS
- 8+ regex patterns untuk extract googlevideo URLs dari berbagai format (escaped, unescaped, dll)
- Handle escaped URL patterns: `\\u003d` → `=`, `\\u0026` → `&`, `\\/` → `/`

**File**: `core/network/EmbedVideoExtractor.kt`

## 14. Anoboy & Oploverz Stream Resolution Fallback

**Masalah**: Jika Blogger embed gagal resolve, tidak ada fallback. Streams langsung kosong.

**Fix**:
- **Anoboy**: `getStream()` sekarang langsung cek `isDirectVideoUrl()` sebelum resolve embed. Jika embed gagal, masih ada fallback ke `.soraddlx .soraurlx`
- **Oploverz**: `getStream()` menambahkan iframe fallback — jika embed extractor gagal, fetch embed page dan cari iframe lain di dalamnya
- Kedua provider menggunakan `isDirectVideoUrl()` check lebih awal untuk menghindari resolve yang tidak perlu

**File**: `provider/data/AnoboyProvider.kt`, `provider/data/OploverzProvider.kt`

## 15. Samehadaku Home Content Extraction Fix

**Masalah**: Home page SOKUJA menggunakan RSC streaming sehingga main content (Update Terbaru) tidak muncul karena belum di-load di initial HTML.

**Fix**:
- `parseRscLatest()` mencari episode links di seluruh HTML termasuk dalam `<script>` tags
- Fallback regex pattern mencari `"-episode-N-subtitle-indonesia"` di raw HTML
- `parseHeroFromRsc()` extract hero content dari RSC data
- Sidebar "Anime Populer" tetap di-parse dari static HTML

**File**: `provider/data/SamehadakuProvider.kt`

---

# Pengaturan Privasi

ProFlix fokus pada privasi.

**TIDAK boleh menyertakan**:
- Analytics SDK.
- Advertising SDK.
- Tracking SDK.
- Cloud service yang tidak perlu.
- Firebase Analytics.
- Third party trackers.

---

# Performa

## UI
- Gunakan `LazyColumn`, `LazyRow`, `Paging`
- Jangan load semua data sekaligus

## Memori
- Optimize image memory (Coil cache)
- Optimize player lifecycle
- Cancel coroutine yang tidak diperlukan

## Network
- Cache, Pagination, Background loading
- OkHttp connection pooling

---

# Keamanan

- HTTPS only.
- Input validation.
- Secure networking.
- Tidak ada hardcoded secrets atau API keys.

---

# Struktur File Penting

| File | Fungsi |
|---|---|
| `gradle/libs.versions.toml` | Version catalog semua library |
| `gradle/wrapper/gradle-wrapper.properties` | Versi Gradle (8.7) |
| `build.gradle.kts` (root) | Plugin declarations |
| `settings.gradle.kts` | Module includes |
| `app/build.gradle.kts` | App module config |
| `provider/data/AnoboyProvider.kt` | Scraping Anoboy |
| `provider/data/SamehadakuProvider.kt` | Scraping Samehadaku |
| `provider/data/OploverzProvider.kt` | Scraping Oploverz |
| `provider/data/ProviderManager.kt` | Provider switching |
| `provider/domain/Provider.kt` | Provider interface |
| `core/player/WebViewPlayerScreen.kt` | WebView-based player |
| `feature/player/StreamPlayerViewModel.kt` | Stream resolution logic |
| `feature/player/StreamPlayerScreen.kt` | Player UI |
| `feature/player/PlayerNavigation.kt` | Player route definitions |
| `app/navigation/ProFlixNavHost.kt` | Navigation graph |
| `app/navigation/Screen.kt` | Route definitions |

---

# Roadmap Development

## Versi 1.0 (DALAM PROGRESS)

Core Application:
- Project setup multi-module
- Compose UI + Material 3
- Navigation Compose
- Dark theme
- 3 provider aktif (Anoboy, Samehadaku, Oploverz)
- Provider switching via Settings
- Custom domain per provider
- ExoPlayer (Media3) untuk direct URL
- EmbedVideoExtractor untuk resolve embed pages
- Stream source selection (pilih mirror/kualitas)
- Detail screen dengan daftar episode
- Search dengan instant search
- Room database (history, favorites, continue watching)
- DataStore preferences

## Versi 1.5 (RENCANA)

Library Features:
- Riwayat tontonan lengkap
- Favorit dengan sorting
- Continue watching otomatis
- Pencarian lanjutan

## Versi 2.0 (RENCANA)

Premium Experience:
- Animasi lebih halus
- Picture in Picture
- Subtitle manager
- Advanced player controls

## Versi 3.0 (RENCANA)

Advanced Features:
- Multiple provider simultan
- Offline support
- Better recommendations
- Chromecast support

---

# Aturan Coding

**Selalu**:
- Tulis Kotlin yang bersih.
- Gunakan immutable state.
- Prioritaskan Flow.
- Handle semua error.
- Dokumentasi logika kompleks.

**Jangan pernah**:
- Taruh business logic di Composable.
- Buat class yang terlalu besar.
- Abaikan exception.
- Hardcode UI strings.

---

# Aturan AI Assistant

Saat memodifikasi kode:

1. Pahami arsitektur yang sudah ada.
2. Jangan pecah MVVM.
3. Pisahkan modul dengan benar.
4. Jelaskan perubahan besar.
5. Prioritaskan solusi sederhana yang mudah dirawat.
6. Ikuti best practices Android.

---

# Status Build Terakhir

```
BUILD SUCCESSFUL in 2m 2s
505 actionable tasks: 30 executed, 475 up-to-date
```

**Versi Gradle**: 8.7
**Versi AGP**: 8.5.0
**Versi Kotlin**: 2.0.0
**JVM Target**: 17
**Total Modul**: 15

---

# AUDIT PLAN (MUST DO BESOK)

> **PRINSIP INTI**: Jangan pukul rata. Beda provider = beda embed host = beda extractor. Audit satu persatu, pahami, resapi, baru implementasikan.

## Per-Provider Audit Checklist

Untuk SETIAP provider (Samehadaku, Oploverz, Anoboy), lakukan langkah ini:

### Step 1: Halaman Home
- Fetch halaman home via web
- Bandingkan: apa yg tampil di web vs apa yg di-parse app
- Cek: judul, poster, episode terbaru, kategori — sudah match belum?
- Jika beda: catat selector/regex yg salah dan fix

### Step 2: Halaman Detail
- Fetch halaman detail 1 anime
- Bandingkan: judul postingan, info (genre, tahun, rating), daftar episode
- Cek: semua episode muncul? judul episode benar? urutan benar?
- Cek: episode dari anime lain jangan ikut muncul (salah filter)

### Step 3: Halaman Episode / Stream
- Fetch halaman episode yg sedang diputar
- Catat: embed video host apa yg dipake (blogger? turboviplay? sokuja api? upbolt? dll)
- Catat: ada berapa mirror/server? masing2 host apa?
- Test: extract masing2 mirror, apakah bisa resolve ke direct MP4/HLS?

### Step 4: Implement Per-Provider Extractor
- Buat extractor khusus per provider (jangan gabung jadi satu EmbedVideoExtractor)
- Samehadaku: extractor khusus Samehadaku (API sokuja, iframe, dll)
- Oploverz: extractor khusus Oploverz (blogger, upbolt, dll)
- Anoboy: extractor khusus Anoboy (blogger, turboviplay, dll)

### Step 5: Player UI Enhancement
- Di bawah player, tampilkan daftar episode sesuai postingan yg sedang diputar
- Judul postingan harus match dgn yg diputar
- Episode navigation (prev/next) harus sesuai urutan episode postingan

## Detail Per Provider

### Samehadaku (x6.sokuja.uk)
- **Tipe**: Next.js SPA (SOKUJA) + React Server Components
- **Embed**: API endpoint `/api/video-mirrors?e={episodeId}` → direct MP4
- **Extractor**: API-based (bukan iframe scraping)
- **Catatan**: Episode ID numerik di-extract dari RSC data
- **Status**: Perlu audit ulang home layout + episode matching

### Oploverz (oploverz.site)
- **Tipe**: SvelteKit SPA + SSR data
- **Embed**: `pageData.episodes.data[N].streamUrl[]` → array embed URLs
- **Host yg dikenal**: blogger.com (95%), upbolt.to, 4meplayer.pro, filedon.co, abyssplayer.com
- **Extractor**: Per-host extraction (blogger batchexecute, upbolt direct, dll)
- **Catatan**: Jangan pukul rata pakai 1 extractor

### Anoboy (anoboy.pk)
- **Tipe**: WordPress + AnimeStream Theme
- **Embed**: Mirror `<select>` base64-encoded iframe HTML
- **Host yg dikenal**: blogger.com, turboviplay.com, turbovidhls.com
- **Extractor**: Per-host extraction
- **Catatan**: Base64 decode → iframe src → resolve per host

## Player UI Requirements
- [ ] Episode list di bawah player
- [ ] Judul postingan yg benar (match dgn yg diputar)
- [ ] Episode navigation sesuai urutan
- [ ] Source/mirror selector yg jelas
- [ ] Skip opening/ending buttons
- [ ] Auto-play next episode toggle
- [ ] Netflix-like dark design
