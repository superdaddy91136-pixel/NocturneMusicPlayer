# Nocturne — Obsidian Reference Offline Music Player

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.20-blue.svg?logo=kotlin)](http://kotlinlang.org)

**Nocturne** is a standalone, offline-first Android music player built from scratch using modern Android development practices. It is built entirely in **Kotlin** and **Jetpack Compose** and relies on **AndroidX Media3** for playback.

It features a stunning **AMOLED Matte Black (`#050507`) & Selective Liquid-Glass** design language, dynamic album-artwork ambient lighting, a centerpiece **Now Playing** experience, **Synchronized Lyrics**, and advanced audiophile playback controls.

---

## 🎨 Features
- **Modern Jetpack Compose UI**: 100% Compose UI featuring adaptive glass panels, liquid ambient effects, and fluid animations.
- **Offline First**: Scans your local `MediaStore` rapidly. Doesn't require any internet connection or cloud account.
- **Advanced Playback (Media3)**: Powered by ExoPlayer. Supports gapless playback, crossfade, variable playback speed/pitch, and rich audio formats (FLAC, OGG, MP3).
- **Synchronized Lyrics**: Built-in `LrcParser` that supports standard LRC, multi-timestamp, and **Enhanced LRC `<mm:ss.xx>` word-level karaoke**. Supports ID3/MP4 embedded lyrics and local `.lrc` files!
- **Audiophile DSP**: 5-band Equalizer, BassBoost, Virtualizer, and ReplayGain normalization.
- **Theme & Custom Icons**: Deep AMOLED black theme with selective frosted glass. Ships with 16 different launcher icon variations you can choose from the Settings!
- **Privacy First**: Zero trackers, zero analytics. Only requests the permissions it absolutely needs to play your music.

---

## 🛠️ Tech Stack & Architecture
Nocturne follows **Clean Architecture** and **MVVM** with a unidirectional data flow (using `StateFlow` + immutable UI models). 

*   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Media Engine**: [AndroidX Media3 (ExoPlayer & MediaSessionService)](https://developer.android.com/media/media3)
*   **Database**: [Room](https://developer.android.com/training/data-storage/room)
*   **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
*   **Image Loading & Colors**: [Coil](https://coil-kt.github.io/coil/) & [Palette](https://developer.android.com/develop/ui/views/graphics/palette-colors)
*   **Preferences**: [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

---

## 🚀 Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK**: Java 17 (bundled with modern Android Studio).
- **Android SDK**: `compileSdk = 35`, `minSdk = 26` (Android 8.0+).

### Building in Android Studio
1. Clone this repository: `git clone https://github.com/your-username/NocturneMusicPlayer.git`
2. Open **Android Studio** -> **Open** -> select the cloned directory.
3. Let Gradle sync and download dependencies.
4. Select the `app` run configuration and click **Run** (`Shift + F10`).

### Building via Command Line
```bash
./gradlew assembleDebug
```
The compiled debug APK will be written to: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🤝 Contributing

We love pull requests! If you'd like to help improve Nocturne, please check out our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md).

- **Found a bug?** Please open an [Issue](https://github.com/your-username/NocturneMusicPlayer/issues).
- **Have a feature request?** Start a discussion or open an issue.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.