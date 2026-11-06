package com.nocturne.music.model

import androidx.compose.runtime.Immutable

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

enum class SongSortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date Added"),
    DURATION("Duration"),
    YEAR("Release Year"),
    PLAY_COUNT("Most Played")
}

enum class AlbumSortOption(val label: String) {
    TITLE("Album Title"),
    ARTIST("Artist"),
    YEAR("Release Year"),
    SONG_COUNT("Track Count")
}

enum class ArtistSortOption(val label: String) {
    NAME("Artist Name"),
    SONG_COUNT("Track Count"),
    ALBUM_COUNT("Album Count")
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class DynamicBackgroundStyle(val label: String, val description: String) {
    LIQUID_AMBIENT("Liquid Ambient Glow", "Multi-layered radial artwork glow over matte black"),
    DEEP_MATTE("Deep Obsidian Matte", "Subtle top aura with pure AMOLED black lower canvas"),
    PURE_AMOLED("Pure AMOLED Black", "Zero background tinting for maximum contrast and battery savings")
}

enum class LyricsFontSize(val label: String, val activeSp: Float, val inactiveSp: Float) {
    COMPACT("Compact", 20f, 15f),
    BALANCED("Balanced", 24f, 17f),
    EXPRESSIVE("Expressive", 28f, 19f)
}

@Immutable
data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMillibels: Int,
    val minLevelMillibels: Int = -1500,
    val maxLevelMillibels: Int = 1500
) {
    val frequencyLabel: String
        get() = if (centerFreqHz >= 1000) {
            val khz = centerFreqHz / 1000f
            if (centerFreqHz % 1000 == 0) "${centerFreqHz / 1000}kHz" else String.format("%.1fkHz", khz)
        } else {
            "${centerFreqHz}Hz"
        }

    val gainDb: Float
        get() = levelMillibels / 100f
}

@Immutable
data class EqualizerState(
    val enabled: Boolean = false,
    val currentPresetName: String = "Flat Reference",
    val availablePresets: List<String> = listOf(
        "Flat Reference",
        "Obsidian Warmth",
        "Deep Sub-Bass",
        "Vocal Presence",
        "Acoustic Studio",
        "Late Night Detail",
        "Electronic Punch",
        "Custom"
    ),
    val bands: List<EqualizerBand> = defaultBands(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val hardwareAvailable: Boolean = true
) {
    companion object {
        fun defaultBands(): List<EqualizerBand> = listOf(
            EqualizerBand(0, 60, 0),
            EqualizerBand(1, 230, 0),
            EqualizerBand(2, 910, 0),
            EqualizerBand(3, 3600, 0),
            EqualizerBand(4, 14000, 0)
        )

        fun presetCurveMb(presetName: String): List<Int> = when (presetName) {
            "Flat Reference" -> listOf(0, 0, 0, 0, 0)
            "Obsidian Warmth" -> listOf(350, 220, -80, 140, 260)
            "Deep Sub-Bass" -> listOf(600, 380, -120, 80, 180)
            "Vocal Presence" -> listOf(-150, 120, 420, 360, 100)
            "Acoustic Studio" -> listOf(220, 140, 80, 280, 340)
            "Late Night Detail" -> listOf(420, 180, -100, 260, 480)
            "Electronic Punch" -> listOf(520, 240, -180, 300, 450)
            else -> listOf(0, 0, 0, 0, 0)
        }
    }
}

@Immutable
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMs: Long = 0L,
    val totalDurationMinutes: Int = 0,
    val stopAtEndOfTrack: Boolean = false,
    val fadeOutEnabled: Boolean = true
)

@Immutable
data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val equalizerState: EqualizerState = EqualizerState(),
    val audioSessionId: Int = 0,
    val isCrossfading: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val upNextQueue: List<Song>
        get() = if (currentQueueIndex in queue.indices && currentQueueIndex + 1 < queue.size) {
            queue.subList(currentQueueIndex + 1, queue.size)
        } else {
            emptyList()
        }
}

@Immutable
data class UserSettings(
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationSec: Int = 0,
    val isLibraryGridView: Boolean = true,
    val isLibraryLandingGridView: Boolean = true,
    val volumeNormalization: Boolean = false,
    val normalizationPreampDb: Float = 0f,
    val resumeOnBluetoothConnect: Boolean = false,
    val pauseOnZeroVolume: Boolean = true,
    val rememberPlaybackPosition: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoFetchOnlineLyrics: Boolean = false,
    val preferEmbeddedLyrics: Boolean = false,
    val wordLevelKaraokeHighlight: Boolean = true,
    val keepScreenOnDuringLyrics: Boolean = true,
    val lyricsFontSize: LyricsFontSize = LyricsFontSize.BALANCED,
    val dynamicBackgroundStyle: DynamicBackgroundStyle = DynamicBackgroundStyle.LIQUID_AMBIENT,
    val highQualityArtworkCache: Boolean = true,
    val smoothArtworkTransitions: Boolean = true,
    val reducedMotion: Boolean = false,
    val minimumTrackDurationSec: Int = 15,
    val ignoreRingtoneAndNotificationFolders: Boolean = true,
    val songSortOption: SongSortOption = SongSortOption.TITLE,
    val songSortDirection: SortDirection = SortDirection.ASCENDING,
    val albumSortOption: AlbumSortOption = AlbumSortOption.TITLE,
    val artistSortOption: ArtistSortOption = ArtistSortOption.NAME,
    val profileName: String = "Nocturne User",
    val profilePictureUri: String? = null,
    val selectedAppIconId: String = AppIconOption.DEFAULT.id,
    val hasCompletedInitialScan: Boolean = false
)
