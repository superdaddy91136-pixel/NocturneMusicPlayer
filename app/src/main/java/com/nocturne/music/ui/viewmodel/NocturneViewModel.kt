package com.nocturne.music.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.nocturne.music.datastore.UserPreferencesRepository
import com.nocturne.music.designsystem.ArtworkAtmosphere
import com.nocturne.music.library.LibraryRepository
import com.nocturne.music.lyrics.LyricsRepository
import com.nocturne.music.model.Album
import com.nocturne.music.model.AlbumSortOption
import com.nocturne.music.model.Artist
import com.nocturne.music.model.ArtistSortOption
import com.nocturne.music.model.DynamicBackgroundStyle
import com.nocturne.music.model.EqualizerState
import com.nocturne.music.model.Genre
import com.nocturne.music.model.ListeningHistoryEntry
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.LyricsFontSize
import com.nocturne.music.model.LyricsSource
import com.nocturne.music.model.MusicFolder
import com.nocturne.music.model.PlaybackUiState
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.SearchResults
import com.nocturne.music.model.Song
import com.nocturne.music.model.SongSortOption
import com.nocturne.music.model.SortDirection
import com.nocturne.music.model.UserSettings
import com.nocturne.music.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NocturneViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController,
    private val lyricsRepository: LyricsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val artworkManager: com.nocturne.music.library.ArtworkManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playbackController.playbackState

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val equalizerState: StateFlow<EqualizerState> = preferencesRepository.equalizerStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqualizerState())

    val isScanning: StateFlow<Boolean> = libraryRepository.isScanning

    val songs: StateFlow<List<Song>> = libraryRepository.songsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = libraryRepository.albumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = libraryRepository.artistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = libraryRepository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<Genre>> = libraryRepository.genresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = libraryRepository.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = libraryRepository.favoriteSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAlbums: StateFlow<List<Album>> = libraryRepository.favoriteAlbumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteArtists: StateFlow<List<Artist>> = libraryRepository.favoriteArtistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = libraryRepository.recentlyPlayedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = libraryRepository.recentlyAddedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = libraryRepository.mostPlayedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningHistory: StateFlow<List<ListeningHistoryEntry>> = libraryRepository.listeningHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = libraryRepository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedFolders: StateFlow<List<String>> = libraryRepository.excludedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedLyricsCount: StateFlow<Int> = lyricsRepository.cachedLyricsCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _artworkAtmosphere = MutableStateFlow(ArtworkAtmosphere())
    val artworkAtmosphere: StateFlow<ArtworkAtmosphere> = _artworkAtmosphere.asStateFlow()

    private val _currentLyrics = MutableStateFlow(
        LyricsDocument(songId = -1L, isSynced = false, hasWordSync = false, source = LyricsSource.NONE)
    )
    val currentLyrics: StateFlow<LyricsDocument> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<SearchResults> = combine(
        _searchQuery.debounce(120L),
        songs,
        albums,
        artists,
        playlists
    ) { query, allSongs, allAlbums, allArtists, allPlaylists ->
        libraryRepository.searchLibrary(
            query = query,
            songs = allSongs,
            albums = allAlbums,
            artists = allArtists,
            playlists = allPlaylists,
            genres = genres.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        observeCurrentTrackChanges()
    }

    private fun observeCurrentTrackChanges() {
        viewModelScope.launch {
            playbackState
                .map { it.currentSong }
                .distinctUntilChanged { old, new -> old?.id == new?.id && old?.albumArtUri == new?.albumArtUri }
                .collectLatest { song ->
                    _currentLyrics.value = if (song == null) {
                        LyricsDocument(songId = -1L, isSynced = false, hasWordSync = false, source = LyricsSource.NONE)
                    } else {
                        _isLoadingLyrics.value = true
                        val doc = lyricsRepository.loadLyricsForSong(song)
                        _isLoadingLyrics.value = false
                        doc
                    }
                }
        }
    }

    fun scanMediaLibrary() {
        viewModelScope.launch {
            val count = libraryRepository.refreshLibraryFromMediaStore()
            playbackController.restorePreviousSessionIfNeeded()
            if (count > 0) {
                postStatusMessage("Indexed $count audio tracks")
            }
        }
    }

    fun postStatusMessage(msg: String?) {
        _statusMessage.value = msg
    }

    // Playback Controls
    fun playSongs(songList: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        playbackController.playSongs(songList, startIndex, shuffle)
    }

    fun playSong(song: Song, contextQueue: List<Song> = songs.value) {
        val list = if (contextQueue.isNotEmpty()) contextQueue else listOf(song)
        val idx = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackController.playSongs(list, idx, shuffle = false)
    }

    fun shuffleAllSongs() {
        val all = songs.value
        if (all.isNotEmpty()) {
            val randomStart = all.indices.random()
            playbackController.playSongs(all, randomStart, shuffle = true)
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun skipToNext() = playbackController.skipToNext()
    fun skipToPrevious() = playbackController.skipToPrevious()
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeatMode() = playbackController.cycleRepeatMode()
    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatModeState) = playbackController.setPlaybackMode(shuffle, repeatMode)
    fun selectQueueItem(index: Int) = playbackController.selectQueueItem(index)

    fun playNext(song: Song) {
        playbackController.playNext(song)
        postStatusMessage("Playing next: ${song.title}")
    }

    fun addToQueue(song: Song) {
        playbackController.addToQueue(song)
        postStatusMessage("Added to queue: ${song.title}")
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackController.moveQueueItem(fromIndex, toIndex)
    fun removeQueueItem(index: Int) = playbackController.removeQueueItem(index)

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        playbackController.setPlaybackSpeedAndPitch(speed, pitch)
    }

    fun startSleepTimer(minutes: Int, stopAtEndOfTrack: Boolean, fadeOut: Boolean) {
        playbackController.startSleepTimer(minutes, stopAtEndOfTrack, fadeOut)
        val label = if (stopAtEndOfTrack && minutes <= 0) "End of track" else "$minutes min"
        postStatusMessage("Sleep timer set ($label)")
    }

    fun cancelSleepTimer() {
        playbackController.cancelSleepTimer()
        postStatusMessage("Sleep timer cancelled")
    }

    // Favorites & Metadata
    fun toggleSongFavorite(song: Song) {
        viewModelScope.launch {
            val isNowFav = libraryRepository.toggleSongFavorite(song.id)
            playbackController.refreshCurrentSongFavoriteState(song.id, isNowFav)
        }
    }

    fun toggleAlbumFavorite(album: Album) {
        viewModelScope.launch {
            libraryRepository.toggleAlbumFavorite(album.id, album.isFavorite)
        }
    }

    fun toggleArtistFavorite(artist: Artist) {
        viewModelScope.launch {
            libraryRepository.toggleArtistFavorite(artist.id, artist.isFavorite)
        }
    }

    fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int?
    ) {
        viewModelScope.launch {
            libraryRepository.updateSongMetadata(songId, title, artist, album, genre, year)
            postStatusMessage("Metadata updated")
        }
    }

    fun updateCustomArtwork(song: Song, bitmap: android.graphics.Bitmap, embedInFile: Boolean, applyToAlbum: Boolean) {
        viewModelScope.launch {
            val appSidePath = artworkManager.saveCroppedArtworkToAppStorage(song.id, bitmap)
            
            val targetSongs = if (applyToAlbum) {
                albums.value.find { it.id == song.albumId }?.songs ?: listOf(song)
            } else {
                listOf(song)
            }
            
            targetSongs.forEach { targetSong ->
                libraryRepository.setCustomArtwork(targetSong.id, appSidePath)
                if (embedInFile) {
                    val res = artworkManager.writeArtworkToAudioFile(targetSong.filePath, bitmap)
                    if (res.isFailure) {
                        postStatusMessage("Couldn't write to file, saved in app only")
                    }
                }
            }
            
            clearArtworkCache()
            if (!embedInFile) postStatusMessage("Artwork updated")
        }
    }

    fun removeCustomArtwork(song: Song, applyToAlbum: Boolean) {
        viewModelScope.launch {
            val targetSongs = if (applyToAlbum) {
                albums.value.find { it.id == song.albumId }?.songs ?: listOf(song)
            } else {
                listOf(song)
            }
            targetSongs.forEach { targetSong ->
                libraryRepository.removeCustomArtwork(targetSong.id)
            }
            clearArtworkCache()
            postStatusMessage("Custom artwork removed")
        }
    }

    // Playlists
    fun createPlaylist(name: String, description: String = "", initialSongIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            libraryRepository.createPlaylist(name, description, initialSongIds)
            postStatusMessage("Created playlist \"$name\"")
        }
    }

    fun renamePlaylist(playlistId: Long, name: String, description: String) {
        viewModelScope.launch {
            libraryRepository.renamePlaylist(playlistId, name, description)
            postStatusMessage("Updated playlist \"$name\"")
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            libraryRepository.deletePlaylist(playlist.id)
            postStatusMessage("Deleted playlist \"${playlist.name}\"")
        }
    }

    fun addSongToPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch {
            libraryRepository.addSongToPlaylist(playlist.id, song.id)
            postStatusMessage("Added to ${playlist.name}")
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            libraryRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // Search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchQueryToHistory(query: String) {
        viewModelScope.launch {
            libraryRepository.saveSearchQuery(query)
        }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch {
            libraryRepository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            libraryRepository.clearSearchHistory()
        }
    }

    // Lyrics
    fun importLrcFile(uri: Uri) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val doc = lyricsRepository.importLrcFromUri(song.id, uri)
            _currentLyrics.value = doc
            _isLoadingLyrics.value = false
            postStatusMessage(if (doc.lines.isNotEmpty()) "Imported .LRC lyrics" else "Could not parse LRC file")
        }
    }

    fun saveManualLyrics(rawLrcText: String) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val doc = lyricsRepository.saveManualLyricsText(song.id, rawLrcText)
            _currentLyrics.value = doc
            _isLoadingLyrics.value = false
            postStatusMessage("Saved lyrics for ${song.title}")
        }
    }

    fun fetchOnlineLyricsForCurrentSong() {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val doc = lyricsRepository.fetchOnlineLyricsAndCache(song)
            _currentLyrics.value = doc
            _isLoadingLyrics.value = false
            if (doc.source == LyricsSource.NONE) {
                postStatusMessage("No online lyrics found or device is offline")
            } else {
                postStatusMessage("Cached lyrics for offline playback")
            }
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            lyricsRepository.adjustLyricsOffset(song.id, deltaMs)?.let { updated ->
                _currentLyrics.value = updated
            }
        }
    }

    // Equalizer & Settings
    fun updateEqualizer(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoost: Int,
        virtualizer: Int,
        loudnessMb: Int
    ) {
        viewModelScope.launch {
            preferencesRepository.updateEqualizer(
                enabled = enabled,
                presetName = presetName,
                bandLevelsMb = bandLevelsMb,
                bassBoost = bassBoost,
                virtualizer = virtualizer,
                loudnessMb = loudnessMb
            )
        }
    }

    fun setSongSort(option: SongSortOption, direction: SortDirection) {
        viewModelScope.launch { preferencesRepository.setSongSort(option, direction) }
    }

    fun setAlbumSort(option: AlbumSortOption) {
        viewModelScope.launch { preferencesRepository.setAlbumSort(option) }
    }

    fun setArtistSort(option: ArtistSortOption) {
        viewModelScope.launch { preferencesRepository.setArtistSort(option) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setGaplessPlayback(enabled) }
    }

    fun setCrossfadeDurationSec(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setCrossfadeDurationSec(seconds) }
    }

    fun setVolumeNormalization(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVolumeNormalization(enabled) }
    }

    fun setNormalizationPreampDb(db: Float) {
        viewModelScope.launch { preferencesRepository.setNormalizationPreampDb(db) }
    }

    fun setResumeOnBluetooth(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setResumeOnBluetooth(enabled) }
    }

    fun setPauseOnZeroVolume(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPauseOnZeroVolume(enabled) }
    }

    fun setRememberPlaybackPosition(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRememberPlaybackPosition(enabled) }
    }

    fun setAutoFetchOnlineLyrics(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoFetchOnlineLyrics(enabled) }
    }

    fun setPreferEmbeddedLyrics(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPreferEmbeddedLyrics(enabled) }
    }

    fun setWordLevelKaraoke(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWordLevelKaraoke(enabled) }
    }

    fun setKeepScreenOnLyrics(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setKeepScreenOnLyrics(enabled) }
    }

    fun setLyricsFontSize(size: LyricsFontSize) {
        viewModelScope.launch { preferencesRepository.setLyricsFontSize(size) }
    }

    fun setDynamicBackgroundStyle(style: DynamicBackgroundStyle) {
        viewModelScope.launch { preferencesRepository.setDynamicBackgroundStyle(style) }
    }

    fun setHighQualityArtwork(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHighQualityArtwork(enabled) }
    }

    fun setSmoothArtworkTransitions(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSmoothArtworkTransitions(enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReducedMotion(enabled) }
    }

    fun setMinimumTrackDurationSec(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setMinimumTrackDurationSec(seconds)
            libraryRepository.refreshLibraryFromMediaStore()
        }
    }

    fun setIgnoreRingtoneFolders(ignore: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIgnoreRingtoneFolders(ignore)
            libraryRepository.refreshLibraryFromMediaStore()
        }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch {
            libraryRepository.excludeFolder(path)
            postStatusMessage("Excluded folder from library")
        }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch {
            libraryRepository.includeFolder(path)
            postStatusMessage("Restored folder to library")
        }
    }

    fun clearPlayHistory() {
        viewModelScope.launch {
            libraryRepository.clearPlayHistory()
            postStatusMessage("Cleared listening history")
        }
    }

    fun clearLyricsCache() {
        viewModelScope.launch {
            lyricsRepository.clearAllCachedLyrics()
            postStatusMessage("Cleared offline lyrics cache")
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val loader = ImageLoader(appContext)
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
            postStatusMessage("Cleared artwork memory and disk cache")
        }
    }
}
