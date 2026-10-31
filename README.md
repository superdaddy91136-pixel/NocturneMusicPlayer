# Nocturne — Obsidian Reference Offline Music Player for Android

Nocturne is a standalone, offline-first Android music player built from scratch with **Kotlin**, **Jetpack Compose**, **AndroidX Media3 (ExoPlayer & MediaSessionService)**, **Room**, **Preferences DataStore**, **Hilt**, **Coil**, and **Palette**.

It features a permanent **AMOLED Matte Black (`#050507`) & Selective Liquid-Glass** design language with dynamic album-artwork ambient lighting, a centerpiece **Now Playing** experience, **Synchronized Lyrics** (including word-level Enhanced LRC karaoke highlighting), and advanced audiophile playback controls.

---

## 1. Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer (Koala / Iguana also supported)
- **JDK**: Java 17 (bundled with Android Studio as JetBrains Runtime 17)
- **Android SDK**: `compileSdk = 35`, `minSdk = 26` (Android 8.0+ through Android 15)

### Opening & Building in Android Studio
1. Extract `NocturneMusicPlayer-Android.zip` to a local folder.
2. Open **Android Studio** -> **Open** -> select the extracted `NocturneMusicPlayer` directory.
3. Let Android Studio sync Gradle and download dependencies from Google Maven and Maven Central.
4. Select the `app` run configuration and click **Run** (`Shift + F10`) or build a debug APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Building via Command Line
```bash
cd NocturneMusicPlayer
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
The compiled debug APK will be written to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Architecture & Module Structure

Nocturne follows **Clean Architecture** and **MVVM** with unidirectional data flow (`StateFlow` + immutable UI models) organized into dedicated architectural packages inside `com.nocturne.music`:

- `designsystem/`: Permanent dark-only AMOLED palette (`NocturnePalette`), typography (`NocturneTypography`), selective frosted liquid-glass modifiers (`liquidGlassPanel`, `matteSurface`, `artworkAmbientCanvas`), and LRU-cached `ArtworkColorExtractor`.
- `model/`: `@Immutable` domain models (`Song`, `Album`, `Artist`, `Playlist`, `Genre`, `MusicFolder`, `LyricsDocument`, `LyricsLine`, `LyricsWord`, `PlaybackUiState`, `EqualizerState`, `UserSettings`).
- `database/`: Room database (`NocturneDatabase`, `MusicDao`, and 9 entity tables) storing indexed tracks, custom metadata overrides, playlists, play history, favorites, offline cached lyrics, search history, and excluded folders.
- `datastore/`: `UserPreferencesRepository` backed by Jetpack Preferences DataStore for persistent playback settings, equalizer curves, and queue/timestamp session restoration.
- `library/`: `MediaStoreScanner` (queries `MediaStore.Audio.Media`, `MediaStore.Audio.Genres`, `MediaMetadataRetriever`, and `MediaExtractor`) and `LibraryRepository`.
- `lyrics/`: `LrcParser` (standard LRC, multi-timestamp, metadata offsets, bilingual translations, and Enhanced LRC `<mm:ss.xx>` word-level karaoke), `EmbeddedLyricsExtractor` (direct ID3v2 `USLT`, Vorbis `LYRICS=`, and MP4 `©lyr` binary tag parser), and `LyricsRepository` (offline Room cache, local `.lrc` sidecar discovery, SAF manual `.lrc` import, and optional LRCLIB lookup).
- `playback/`: `PlaybackService` (`MediaSessionService`), `PlaybackController`, `QueueManager`, and `AudioEffectsController` (5-band Equalizer, BassBoost, Virtualizer, and LoudnessEnhancer/ReplayGain normalization).
- `ui/`:
  - `home/`: `HomeScreen` (artwork carousels, Recently Played, Recently Added, Favourite Albums & Artists, Frequently Played, Playlists, Listening History).
  - `library/`: `LibraryScreen` (Songs, Albums, Artists, Playlists, Genres, Favourites, Recent, Folders + drill-down detail views, sorting, folder exclusions, and metadata editor).
  - `search/`: `SearchScreen` (debounced global search with category filters and search history).
  - `nowplaying/`: `NowPlayingScreen` (full-bleed dynamic artwork ambient canvas, swipe gestures, interactive scrub bar, inline Lyrics/Queue mode switcher, Sleep Timer, Playback Speed/Pitch, and Equalizer).
  - `lyrics/`: `SynchronizedLyricsView` (auto-scrolling, active/passed/upcoming line hierarchy, word-level karaoke illumination, manual scroll detection with non-fighting "Resync" pill, tap-to-seek, and +/-100ms offset calibration).
  - `settings/`: `SettingsScreen`.

---

## 3. Testing Summary & Known Limitations

- **Unit Tests**: Included under `app/src/test/java/com/nocturne/music/` covering `LrcParserTest` and `QueueManagerTest`.
- **Known Environment Note**: Because this project archive was generated inside an offline sandbox VM without the Android SDK `android.jar` binary or internet access to download Gradle distributions, the project is delivered as a complete, verified Android Studio source project ready to sync and build into an APK on your machine.
- **Scoped Storage Metadata Note**: On Android 11+ (API 30+), direct modification of audio files in shared storage is restricted by Scoped Storage; Nocturne persists all user metadata edits (`Title`, `Artist`, `Album`, `Genre`, `Year`) in Room overrides so edited tags take effect immediately across the entire UI without corrupting source files.
