package com.nocturne.music.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nocturne.music.model.AlbumSortOption
import com.nocturne.music.model.ArtistSortOption
import com.nocturne.music.model.DynamicBackgroundStyle
import com.nocturne.music.model.EqualizerState
import com.nocturne.music.model.LyricsFontSize
import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.SongSortOption
import com.nocturne.music.model.SortDirection
import com.nocturne.music.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nocturneDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nocturne_preferences"
)

data class PersistedPlaybackSession(
    val queueSongIds: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.nocturneDataStore

    private object Keys {
        val HAS_COMPLETED_INITIAL_SCAN = booleanPreferencesKey("has_completed_initial_scan")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val IS_LIBRARY_GRID_VIEW = booleanPreferencesKey("is_library_grid_view")
        val IS_LIBRARY_LANDING_GRID_VIEW = booleanPreferencesKey("is_library_landing_grid_view")
        val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization")
        val NORMALIZATION_PREAMP_DB = floatPreferencesKey("normalization_preamp_db")
        val RESUME_ON_BLUETOOTH = booleanPreferencesKey("resume_on_bluetooth")
        val PAUSE_ON_ZERO_VOLUME = booleanPreferencesKey("pause_on_zero_volume")
        val REMEMBER_PLAYBACK_POSITION = booleanPreferencesKey("remember_playback_position")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")

        val AUTO_FETCH_ONLINE_LYRICS = booleanPreferencesKey("auto_fetch_online_lyrics")
        val PREFER_EMBEDDED_LYRICS = booleanPreferencesKey("prefer_embedded_lyrics")
        val WORD_LEVEL_KARAOKE = booleanPreferencesKey("word_level_karaoke")
        val KEEP_SCREEN_ON_LYRICS = booleanPreferencesKey("keep_screen_on_lyrics")
        val LYRICS_FONT_SIZE = stringPreferencesKey("lyrics_font_size")

        val DYNAMIC_BG_STYLE = stringPreferencesKey("dynamic_bg_style")
        val HIGH_QUALITY_ARTWORK = booleanPreferencesKey("high_quality_artwork")
        val SMOOTH_ARTWORK_TRANSITIONS = booleanPreferencesKey("smooth_artwork_transitions")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")

        val MIN_TRACK_DURATION_SEC = intPreferencesKey("min_track_duration_sec")
        val IGNORE_RINGTONES = booleanPreferencesKey("ignore_ringtones")

        val SONG_SORT_OPTION = stringPreferencesKey("song_sort_option")
        val SONG_SORT_DIRECTION = stringPreferencesKey("song_sort_direction")
        val ALBUM_SORT_OPTION = stringPreferencesKey("album_sort_option")
        val ARTIST_SORT_OPTION = stringPreferencesKey("artist_sort_option")

        // Equalizer state
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_LOUDNESS_MB = intPreferencesKey("eq_loudness_mb")

        // Playback session restoration
        val SAVED_QUEUE_IDS = stringPreferencesKey("saved_queue_ids")
        val SAVED_QUEUE_INDEX = intPreferencesKey("saved_queue_index")
        val SAVED_POSITION_MS = longPreferencesKey("saved_position_ms")
        val SAVED_SHUFFLE = booleanPreferencesKey("saved_shuffle")
        val SAVED_REPEAT_MODE = stringPreferencesKey("saved_repeat_mode")
        val SAVED_SPEED = floatPreferencesKey("saved_speed")
        val SAVED_PITCH = floatPreferencesKey("saved_pitch")
        
        val USER_PROFILE_NAME = stringPreferencesKey("user_profile_name")
        val USER_PROFILE_PICTURE_URI = stringPreferencesKey("user_profile_picture_uri")
        val SELECTED_APP_ICON = stringPreferencesKey("selected_app_icon")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserSettings(
                gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
                crossfadeDurationSec = prefs[Keys.CROSSFADE_SEC] ?: 0,
                isLibraryGridView = prefs[Keys.IS_LIBRARY_GRID_VIEW] ?: true,
                isLibraryLandingGridView = prefs[Keys.IS_LIBRARY_LANDING_GRID_VIEW] ?: true,
                volumeNormalization = prefs[Keys.VOLUME_NORMALIZATION] ?: false,
                normalizationPreampDb = prefs[Keys.NORMALIZATION_PREAMP_DB] ?: 0f,
                resumeOnBluetoothConnect = prefs[Keys.RESUME_ON_BLUETOOTH] ?: false,
                pauseOnZeroVolume = prefs[Keys.PAUSE_ON_ZERO_VOLUME] ?: true,
                rememberPlaybackPosition = prefs[Keys.REMEMBER_PLAYBACK_POSITION] ?: true,
                defaultPlaybackSpeed = prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
                autoFetchOnlineLyrics = prefs[Keys.AUTO_FETCH_ONLINE_LYRICS] ?: false,
                preferEmbeddedLyrics = prefs[Keys.PREFER_EMBEDDED_LYRICS] ?: false,
                wordLevelKaraokeHighlight = prefs[Keys.WORD_LEVEL_KARAOKE] ?: true,
                keepScreenOnDuringLyrics = prefs[Keys.KEEP_SCREEN_ON_LYRICS] ?: true,
                lyricsFontSize = runCatching {
                    LyricsFontSize.valueOf(prefs[Keys.LYRICS_FONT_SIZE] ?: LyricsFontSize.BALANCED.name)
                }.getOrDefault(LyricsFontSize.BALANCED),
                dynamicBackgroundStyle = runCatching {
                    DynamicBackgroundStyle.valueOf(
                        prefs[Keys.DYNAMIC_BG_STYLE] ?: DynamicBackgroundStyle.LIQUID_AMBIENT.name
                    )
                }.getOrDefault(DynamicBackgroundStyle.LIQUID_AMBIENT),
                highQualityArtworkCache = prefs[Keys.HIGH_QUALITY_ARTWORK] ?: true,
                smoothArtworkTransitions = prefs[Keys.SMOOTH_ARTWORK_TRANSITIONS] ?: true,
                reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
                minimumTrackDurationSec = prefs[Keys.MIN_TRACK_DURATION_SEC] ?: 15,
                ignoreRingtoneAndNotificationFolders = prefs[Keys.IGNORE_RINGTONES] ?: true,
                songSortOption = runCatching {
                    SongSortOption.valueOf(prefs[Keys.SONG_SORT_OPTION] ?: SongSortOption.TITLE.name)
                }.getOrDefault(SongSortOption.TITLE),
                songSortDirection = runCatching {
                    SortDirection.valueOf(prefs[Keys.SONG_SORT_DIRECTION] ?: SortDirection.ASCENDING.name)
                }.getOrDefault(SortDirection.ASCENDING),
                albumSortOption = runCatching {
                    AlbumSortOption.valueOf(prefs[Keys.ALBUM_SORT_OPTION] ?: AlbumSortOption.TITLE.name)
                }.getOrDefault(AlbumSortOption.TITLE),
                artistSortOption = runCatching {
                    ArtistSortOption.valueOf(prefs[Keys.ARTIST_SORT_OPTION] ?: ArtistSortOption.NAME.name)
                }.getOrDefault(ArtistSortOption.NAME),
                profileName = prefs[Keys.USER_PROFILE_NAME] ?: "Nocturne User",
                profilePictureUri = prefs[Keys.USER_PROFILE_PICTURE_URI],
                selectedAppIconId = prefs[Keys.SELECTED_APP_ICON] ?: com.nocturne.music.model.AppIconOption.DEFAULT.id,
                hasCompletedInitialScan = prefs[Keys.HAS_COMPLETED_INITIAL_SCAN] ?: false
            )
        }

    val equalizerStateFlow: Flow<EqualizerState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val enabled = prefs[Keys.EQ_ENABLED] ?: false
            val preset = prefs[Keys.EQ_PRESET] ?: "Flat Reference"
            val csv = prefs[Keys.EQ_BANDS_CSV]
            val parsedLevels = csv?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                ?: EqualizerState.presetCurveMb(preset)
            val defaultBands = EqualizerState.defaultBands().mapIndexed { idx, band ->
                band.copy(levelMillibels = parsedLevels.getOrElse(idx) { 0 })
            }
            EqualizerState(
                enabled = enabled,
                currentPresetName = preset,
                bands = defaultBands,
                bassBoostStrength = prefs[Keys.EQ_BASS_BOOST] ?: 0,
                virtualizerStrength = prefs[Keys.EQ_VIRTUALIZER] ?: 0,
                loudnessGainMb = prefs[Keys.EQ_LOUDNESS_MB] ?: 0
            )
        }

    suspend fun getPersistedPlaybackSession(): PersistedPlaybackSession {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val idsString = prefs[Keys.SAVED_QUEUE_IDS].orEmpty()
        val ids = idsString.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        val index = prefs[Keys.SAVED_QUEUE_INDEX] ?: 0
        val position = prefs[Keys.SAVED_POSITION_MS] ?: 0L
        val shuffle = prefs[Keys.SAVED_SHUFFLE] ?: false
        val repeat = runCatching {
            RepeatModeState.valueOf(prefs[Keys.SAVED_REPEAT_MODE] ?: RepeatModeState.OFF.name)
        }.getOrDefault(RepeatModeState.OFF)
        val speed = prefs[Keys.SAVED_SPEED] ?: 1.0f
        val pitch = prefs[Keys.SAVED_PITCH] ?: 1.0f
        return PersistedPlaybackSession(
            queueSongIds = ids,
            currentIndex = index,
            positionMs = position,
            shuffleEnabled = shuffle,
            repeatMode = repeat,
            playbackSpeed = speed,
            playbackPitch = pitch
        )
    }

    suspend fun savePlaybackSession(
        queueSongIds: List<Long>,
        currentIndex: Int,
        positionMs: Long,
        shuffleEnabled: Boolean,
        repeatMode: RepeatModeState,
        playbackSpeed: Float,
        playbackPitch: Float
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.SAVED_QUEUE_IDS] = queueSongIds.take(500).joinToString(",")
            prefs[Keys.SAVED_QUEUE_INDEX] = currentIndex.coerceAtLeast(0)
            prefs[Keys.SAVED_POSITION_MS] = positionMs.coerceAtLeast(0L)
            prefs[Keys.SAVED_SHUFFLE] = shuffleEnabled
            prefs[Keys.SAVED_REPEAT_MODE] = repeatMode.name
            prefs[Keys.SAVED_SPEED] = playbackSpeed
            prefs[Keys.SAVED_PITCH] = playbackPitch
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setCrossfadeDurationSec(seconds: Int) {
        dataStore.edit { it[Keys.CROSSFADE_SEC] = seconds.coerceIn(0, 12) }
    }

    suspend fun setIsLibraryGridView(isGrid: Boolean) {
        dataStore.edit { it[Keys.IS_LIBRARY_GRID_VIEW] = isGrid }
    }

    suspend fun setIsLibraryLandingGridView(isGrid: Boolean) {
        dataStore.edit { it[Keys.IS_LIBRARY_LANDING_GRID_VIEW] = isGrid }
    }

    suspend fun setVolumeNormalization(enabled: Boolean) {
        dataStore.edit { it[Keys.VOLUME_NORMALIZATION] = enabled }
    }

    suspend fun setNormalizationPreampDb(db: Float) {
        dataStore.edit { it[Keys.NORMALIZATION_PREAMP_DB] = db.coerceIn(-6f, 6f) }
    }

    suspend fun setResumeOnBluetooth(enabled: Boolean) {
        dataStore.edit { it[Keys.RESUME_ON_BLUETOOTH] = enabled }
    }

    suspend fun setPauseOnZeroVolume(enabled: Boolean) {
        dataStore.edit { it[Keys.PAUSE_ON_ZERO_VOLUME] = enabled }
    }

    suspend fun setRememberPlaybackPosition(enabled: Boolean) {
        dataStore.edit { it[Keys.REMEMBER_PLAYBACK_POSITION] = enabled }
    }

    suspend fun setAutoFetchOnlineLyrics(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_FETCH_ONLINE_LYRICS] = enabled }
    }

    suspend fun setPreferEmbeddedLyrics(enabled: Boolean) {
        dataStore.edit { it[Keys.PREFER_EMBEDDED_LYRICS] = enabled }
    }

    suspend fun setWordLevelKaraoke(enabled: Boolean) {
        dataStore.edit { it[Keys.WORD_LEVEL_KARAOKE] = enabled }
    }

    suspend fun setKeepScreenOnLyrics(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON_LYRICS] = enabled }
    }

    suspend fun setLyricsFontSize(size: LyricsFontSize) {
        dataStore.edit { it[Keys.LYRICS_FONT_SIZE] = size.name }
    }

    suspend fun setDynamicBackgroundStyle(style: DynamicBackgroundStyle) {
        dataStore.edit { it[Keys.DYNAMIC_BG_STYLE] = style.name }
    }

    suspend fun setHighQualityArtwork(enabled: Boolean) {
        dataStore.edit { it[Keys.HIGH_QUALITY_ARTWORK] = enabled }
    }

    suspend fun setSmoothArtworkTransitions(enabled: Boolean) {
        dataStore.edit { it[Keys.SMOOTH_ARTWORK_TRANSITIONS] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setMinimumTrackDurationSec(seconds: Int) {
        dataStore.edit { it[Keys.MIN_TRACK_DURATION_SEC] = seconds.coerceIn(0, 120) }
    }

    suspend fun setIgnoreRingtoneFolders(ignore: Boolean) {
        dataStore.edit { it[Keys.IGNORE_RINGTONES] = ignore }
    }

    suspend fun setSongSort(option: SongSortOption, direction: SortDirection) {
        dataStore.edit {
            it[Keys.SONG_SORT_OPTION] = option.name
            it[Keys.SONG_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun setAlbumSort(option: AlbumSortOption) {
        dataStore.edit { it[Keys.ALBUM_SORT_OPTION] = option.name }
    }

    suspend fun setArtistSort(option: ArtistSortOption) {
        dataStore.edit { it[Keys.ARTIST_SORT_OPTION] = option.name }
    }

    suspend fun setProfileInfo(name: String, pictureUri: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_PROFILE_NAME] = name
            if (pictureUri != null) {
                prefs[Keys.USER_PROFILE_PICTURE_URI] = pictureUri
            } else {
                prefs.remove(Keys.USER_PROFILE_PICTURE_URI)
            }
        }
    }

    suspend fun setSelectedAppIcon(iconId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_APP_ICON] = iconId
        }
    }

    suspend fun setHasCompletedInitialScan(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_INITIAL_SCAN] = completed
        }
    }

    suspend fun updateEqualizer(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoost: Int,
        virtualizer: Int,
        loudnessMb: Int
    ) {
        dataStore.edit {
            it[Keys.EQ_ENABLED] = enabled
            it[Keys.EQ_PRESET] = presetName
            it[Keys.EQ_BANDS_CSV] = bandLevelsMb.joinToString(",")
            it[Keys.EQ_BASS_BOOST] = bassBoost.coerceIn(0, 1000)
            it[Keys.EQ_VIRTUALIZER] = virtualizer.coerceIn(0, 1000)
            it[Keys.EQ_LOUDNESS_MB] = loudnessMb.coerceIn(0, 1500)
        }
    }
}
