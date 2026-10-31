package com.nocturne.music.library

import com.nocturne.music.database.ExcludedFolderEntity
import com.nocturne.music.database.FavoriteAlbumEntity
import com.nocturne.music.database.FavoriteArtistEntity
import com.nocturne.music.database.MusicDao
import com.nocturne.music.database.PlayHistoryEntity
import com.nocturne.music.database.PlaylistEntity
import com.nocturne.music.database.PlaylistSongCrossRef
import com.nocturne.music.database.SearchHistoryEntity
import com.nocturne.music.datastore.UserPreferencesRepository
import com.nocturne.music.model.Album
import com.nocturne.music.model.AlbumSortOption
import com.nocturne.music.model.Artist
import com.nocturne.music.model.ArtistSortOption
import com.nocturne.music.model.Genre
import com.nocturne.music.model.ListeningHistoryEntry
import com.nocturne.music.model.MusicFolder
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.SearchResults
import com.nocturne.music.model.Song
import com.nocturne.music.model.SongSortOption
import com.nocturne.music.model.SortDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val musicDao: MusicDao,
    private val mediaStoreScanner: MediaStoreScanner,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanCount = MutableStateFlow(0)
    val lastScanCount: StateFlow<Int> = _lastScanCount.asStateFlow()

    private val rawSongsFlow: Flow<List<Song>> = combine(
        musicDao.observeAllSongs(),
        musicDao.observeAllCustomArtwork()
    ) { entities, customArt ->
        val artMap = customArt.associateBy { it.songId }
        entities.map { entity ->
            val model = entity.toDomainModel()
            artMap[model.id]?.let { custom ->
                model.copy(
                    albumArtUri = custom.artworkPath,
                    customArtworkUpdatedAtMs = custom.updatedAtMs
                )
            } ?: model
        }
    }.flowOn(Dispatchers.Default)

    val songsFlow: Flow<List<Song>> = combine(
        rawSongsFlow,
        preferencesRepository.userSettingsFlow
    ) { songs, settings ->
        sortSongs(songs, settings.songSortOption, settings.songSortDirection)
    }.flowOn(Dispatchers.Default)

    val favoriteSongsFlow: Flow<List<Song>> = rawSongsFlow.map { songs ->
        songs.filter { it.isFavorite }.sortedBy { it.title.lowercase() }
    }.flowOn(Dispatchers.Default)

    val recentlyPlayedSongsFlow: Flow<List<Song>> = rawSongsFlow.map { songs ->
        songs.filter { it.lastPlayedAtMs > 0 }
             .sortedByDescending { it.lastPlayedAtMs }
             .take(35)
    }.flowOn(Dispatchers.Default)

    val recentlyAddedSongsFlow: Flow<List<Song>> = rawSongsFlow.map { songs ->
        songs.sortedByDescending { it.dateAddedSec }.take(35)
    }.flowOn(Dispatchers.Default)

    val mostPlayedSongsFlow: Flow<List<Song>> = rawSongsFlow.map { songs ->
        songs.filter { it.playCount > 0 }
             .sortedWith(compareByDescending<Song> { it.playCount }.thenByDescending { it.lastPlayedAtMs })
             .take(35)
    }.flowOn(Dispatchers.Default)

    val albumsFlow: Flow<List<Album>> = combine(
        rawSongsFlow,
        musicDao.observeFavoriteAlbumIds(),
        preferencesRepository.userSettingsFlow
    ) { songs, favAlbumIds, settings ->
        val favSet = favAlbumIds.toHashSet()
        val grouped = songs.groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val sortedTracks = albumSongs.sortedWith(
                    compareBy<Song> { it.discNumber }.thenBy { it.trackNumber }.thenBy { it.title.lowercase() }
                )
                val first = sortedTracks.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    artistId = first.artistId,
                    year = sortedTracks.maxOfOrNull { it.year } ?: 0,
                    songCount = sortedTracks.size,
                    totalDurationMs = sortedTracks.sumOf { it.durationMs },
                    albumArtUri = sortedTracks.firstNotNullOfOrNull { it.albumArtUri },
                    isFavorite = albumId in favSet,
                    songs = sortedTracks
                )
            }
        sortAlbums(grouped, settings.albumSortOption)
    }.flowOn(Dispatchers.Default)

    val favoriteAlbumsFlow: Flow<List<Album>> = albumsFlow.map { albums ->
        albums.filter { it.isFavorite }
    }

    val artistsFlow: Flow<List<Artist>> = combine(
        rawSongsFlow,
        albumsFlow,
        musicDao.observeFavoriteArtistIds(),
        preferencesRepository.userSettingsFlow
    ) { songs, albums, favArtistIds, settings ->
        val favSet = favArtistIds.toHashSet()
        val albumsByArtist = albums.groupBy { it.artistId }
        val grouped = songs.groupBy { it.artistId }
            .map { (artistId, artistSongs) ->
                val first = artistSongs.first()
                val artistAlbums = albumsByArtist[artistId].orEmpty()
                Artist(
                    id = artistId,
                    name = first.artist,
                    albumCount = artistAlbums.size.coerceAtLeast(1),
                    songCount = artistSongs.size,
                    totalDurationMs = artistSongs.sumOf { it.durationMs },
                    artworkUri = artistSongs.firstNotNullOfOrNull { it.albumArtUri },
                    isFavorite = artistId in favSet,
                    albums = artistAlbums,
                    songs = artistSongs.sortedBy { it.title.lowercase() }
                )
            }
        sortArtists(grouped, settings.artistSortOption)
    }.flowOn(Dispatchers.Default)

    val favoriteArtistsFlow: Flow<List<Artist>> = artistsFlow.map { artists ->
        artists.filter { it.isFavorite }
    }

    val playlistsFlow: Flow<List<Playlist>> = combine(
        musicDao.observePlaylists(),
        musicDao.observeAllPlaylistCrossRefs(),
        rawSongsFlow
    ) { playlists, crossRefs, songs ->
        val songMap = songs.associateBy { it.id }
        val refsByPlaylist = crossRefs.groupBy { it.playlistId }
        playlists.map { entity ->
            val orderedSongs = refsByPlaylist[entity.playlistId]
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { ref -> songMap[ref.songId] }
            Playlist(
                id = entity.playlistId,
                name = entity.name,
                description = entity.description,
                createdAtMs = entity.createdAtMs,
                updatedAtMs = entity.updatedAtMs,
                songCount = orderedSongs.size,
                totalDurationMs = orderedSongs.sumOf { it.durationMs },
                coverArtUri = entity.customCoverUri ?: orderedSongs.firstNotNullOfOrNull { it.albumArtUri },
                songs = orderedSongs
            )
        }
    }.flowOn(Dispatchers.Default)

    val genresFlow: Flow<List<Genre>> = rawSongsFlow.map { songs ->
        songs.groupBy { it.genre.ifBlank { "Unspecified" } }
            .map { (genreName, genreSongs) ->
                Genre(
                    name = genreName,
                    songCount = genreSongs.size,
                    totalDurationMs = genreSongs.sumOf { it.durationMs },
                    artworkUri = genreSongs.firstNotNullOfOrNull { it.albumArtUri },
                    songs = genreSongs.sortedBy { it.title.lowercase() }
                )
            }
            .sortedByDescending { it.songCount }
    }.flowOn(Dispatchers.Default)

    val foldersFlow: Flow<List<MusicFolder>> = combine(
        rawSongsFlow,
        musicDao.observeExcludedFolders()
    ) { songs, excludedEntities ->
        val excludedPaths = excludedEntities.map { it.folderPath }.toSet()
        val activeFolders = songs.groupBy { it.folderPath }
            .map { (path, folderSongs) ->
                val folderName = folderSongs.firstOrNull()?.folderName
                    ?: File(path).name.ifBlank { path }
                MusicFolder(
                    path = path,
                    name = folderName,
                    songCount = folderSongs.size,
                    totalDurationMs = folderSongs.sumOf { it.durationMs },
                    isExcluded = path in excludedPaths,
                    songs = folderSongs.sortedBy { it.title.lowercase() }
                )
            }
            .sortedBy { it.name.lowercase() }

        val excludedOnly = excludedPaths
            .filter { path -> activeFolders.none { it.path == path } }
            .map { path ->
                MusicFolder(
                    path = path,
                    name = File(path).name.ifBlank { path },
                    songCount = 0,
                    totalDurationMs = 0L,
                    isExcluded = true,
                    songs = emptyList()
                )
            }
        activeFolders + excludedOnly
    }.flowOn(Dispatchers.Default)

    val listeningHistoryFlow: Flow<List<ListeningHistoryEntry>> = combine(
        musicDao.observePlayHistory(100),
        rawSongsFlow
    ) { historyEntities, songs ->
        val songMap = songs.associateBy { it.id }
        historyEntities.mapNotNull { entry ->
            val song = songMap[entry.songId] ?: return@mapNotNull null
            ListeningHistoryEntry(
                historyId = entry.historyId,
                song = song,
                playedAtMs = entry.playedAtMs,
                listenedDurationMs = entry.listenedDurationMs
            )
        }
    }.flowOn(Dispatchers.Default)

    val searchHistoryFlow: Flow<List<String>> = musicDao.observeSearchHistory()
        .map { list -> list.map { it.query } }

    val excludedFoldersFlow: Flow<List<String>> = musicDao.observeExcludedFolders()
        .map { list -> list.map { it.folderPath } }

    suspend fun refreshLibraryFromMediaStore(): Int = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext _lastScanCount.value
        _isScanning.value = true
        try {
            val settings = preferencesRepository.userSettingsFlow.first()
            val excluded = musicDao.getExcludedFolderPathsOnce().toSet()
            val existingSongs = musicDao.getAllSongsOnce().associateBy { it.id }

            val scannedEntities = mediaStoreScanner.scanDeviceMusic(
                minDurationSec = settings.minimumTrackDurationSec,
                ignoreRingtones = settings.ignoreRingtoneAndNotificationFolders,
                excludedFolders = excluded,
                existingSongsById = existingSongs
            )

            if (scannedEntities.isNotEmpty()) {
                musicDao.upsertSongs(scannedEntities)
                musicDao.deleteSongsNotIn(scannedEntities.map { it.id })
            } else if (excluded.isNotEmpty()) {
                musicDao.clearAllSongs()
            }

            _lastScanCount.value = scannedEntities.size
            scannedEntities.size
        } finally {
            _isScanning.value = false
        }
    }

    fun searchLibrary(
        query: String,
        songs: List<Song>,
        albums: List<Album>,
        artists: List<Artist>,
        playlists: List<Playlist>,
        genres: List<Genre>
    ): SearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchResults()
        val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        val matchedSongs = songs.filter { song ->
            val hay = "${song.title} ${song.artist} ${song.album} ${song.genre} ${song.folderName}".lowercase()
            tokens.all { hay.contains(it) }
        }.take(40)

        val matchedAlbums = albums.filter { album ->
            val hay = "${album.title} ${album.artist} ${album.year}".lowercase()
            tokens.all { hay.contains(it) }
        }.take(20)

        val matchedArtists = artists.filter { artist ->
            val hay = artist.name.lowercase()
            tokens.all { hay.contains(it) }
        }.take(20)

        val matchedPlaylists = playlists.filter { playlist ->
            val hay = "${playlist.name} ${playlist.description}".lowercase()
            tokens.all { hay.contains(it) }
        }.take(15)

        val matchedGenres = genres.filter { genre ->
            val hay = genre.name.lowercase()
            tokens.all { hay.contains(it) }
        }.take(15)

        return SearchResults(
            query = trimmed,
            songs = matchedSongs,
            albums = matchedAlbums,
            artists = matchedArtists,
            playlists = matchedPlaylists,
            genres = matchedGenres
        )
    }

    suspend fun toggleSongFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val song = musicDao.getSongById(songId) ?: return@withContext false
        val newFav = !song.isFavorite
        musicDao.updateSongFavorite(songId, newFav)
        newFav
    }

    suspend fun toggleAlbumFavorite(albumId: Long, currentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (currentlyFavorite) {
            musicDao.removeFavoriteAlbum(albumId)
        } else {
            musicDao.addFavoriteAlbum(FavoriteAlbumEntity(albumId = albumId))
        }
    }

    suspend fun toggleArtistFavorite(artistId: Long, currentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (currentlyFavorite) {
            musicDao.removeFavoriteArtist(artistId)
        } else {
            musicDao.addFavoriteArtist(FavoriteArtistEntity(artistId = artistId))
        }
    }

    suspend fun recordSongPlayed(songId: Long, listenedDurationMs: Long = 0L) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        musicDao.incrementPlayCount(songId, now)
        musicDao.insertPlayHistory(
            PlayHistoryEntity(
                songId = songId,
                playedAtMs = now,
                listenedDurationMs = listenedDurationMs
            )
        )
    }

    suspend fun createPlaylist(name: String, description: String = "", initialSongIds: List<Long> = emptyList()): Long =
        withContext(Dispatchers.IO) {
            val trimmedName = name.trim().ifBlank { "Untitled Playlist" }
            val playlistId = musicDao.insertPlaylist(
                PlaylistEntity(
                    name = trimmedName,
                    description = description.trim()
                )
            )
            if (initialSongIds.isNotEmpty()) {
                val crossRefs = initialSongIds.distinct().mapIndexed { index, songId ->
                    PlaylistSongCrossRef(
                        playlistId = playlistId,
                        songId = songId,
                        position = index
                    )
                }
                musicDao.insertPlaylistSongCrossRefs(crossRefs)
            }
            playlistId
        }

    suspend fun renamePlaylist(playlistId: Long, newName: String, newDescription: String) = withContext(Dispatchers.IO) {
        musicDao.renamePlaylist(
            playlistId = playlistId,
            name = newName.trim().ifBlank { "Untitled Playlist" },
            description = newDescription.trim(),
            updatedAtMs = System.currentTimeMillis()
        )
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.clearPlaylistSongs(playlistId)
        musicDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val nextPos = musicDao.getMaxPositionInPlaylist(playlistId) + 1
        musicDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = nextPos
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        musicDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) = withContext(Dispatchers.IO) {
        musicDao.clearPlaylistSongs(playlistId)
        val crossRefs = orderedSongIds.mapIndexed { idx, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = idx
            )
        }
        musicDao.insertPlaylistSongCrossRefs(crossRefs)
    }

    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int?
    ) = withContext(Dispatchers.IO) {
        musicDao.updateSongMetadataOverrides(
            songId = songId,
            title = title.trim().takeIf { it.isNotBlank() },
            artist = artist.trim().takeIf { it.isNotBlank() },
            album = album.trim().takeIf { it.isNotBlank() },
            genre = genre.trim().takeIf { it.isNotBlank() },
            year = year?.takeIf { it > 0 }
        )
    }

    suspend fun setCustomArtwork(songId: Long, artworkPath: String) = withContext(Dispatchers.IO) {
        musicDao.upsertCustomArtwork(com.nocturne.music.database.CustomArtworkEntity(songId, artworkPath))
    }

    suspend fun removeCustomArtwork(songId: Long) = withContext(Dispatchers.IO) {
        musicDao.deleteCustomArtwork(songId)
    }

    suspend fun enrichSongAudioSpecs(song: Song) = withContext(Dispatchers.IO) {
        val specs = mediaStoreScanner.inspectExtendedAudioSpecs(song.contentUri)
        musicDao.updateExtendedAudioSpecs(
            songId = song.id,
            bitrateKbps = specs.bitrateKbps.takeIf { it > 0 } ?: song.bitrateKbps,
            sampleRateHz = specs.sampleRateHz,
            channels = specs.channels,
            replayGainDb = specs.replayGainDb
        )
    }

    suspend fun excludeFolder(folderPath: String) = withContext(Dispatchers.IO) {
        musicDao.addExcludedFolder(ExcludedFolderEntity(folderPath = folderPath))
        refreshLibraryFromMediaStore()
    }

    suspend fun includeFolder(folderPath: String) = withContext(Dispatchers.IO) {
        musicDao.removeExcludedFolder(folderPath)
        refreshLibraryFromMediaStore()
    }

    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            musicDao.upsertSearchQuery(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        musicDao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        musicDao.clearSearchHistory()
    }

    suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        musicDao.clearPlayHistory()
    }

    suspend fun getSongsByIdsOrdered(songIds: List<Long>): List<Song> = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext emptyList()
        val map = musicDao.getSongsByIds(songIds).associate { it.id to it.toDomainModel() }
        songIds.mapNotNull { map[it] }
    }

    companion object {
        fun sortSongs(
            songs: List<Song>,
            option: SongSortOption,
            direction: SortDirection
        ): List<Song> {
            val comparator: Comparator<Song> = when (option) {
                SongSortOption.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                SongSortOption.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
                SongSortOption.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                SongSortOption.DATE_ADDED -> compareBy { it.dateAddedSec }
                SongSortOption.DURATION -> compareBy { it.durationMs }
                SongSortOption.YEAR -> compareBy { it.year }
                SongSortOption.PLAY_COUNT -> compareBy { it.playCount }
            }
            val finalComparator = if (direction == SortDirection.DESCENDING) {
                comparator.reversed()
            } else {
                comparator
            }
            return songs.sortedWith(finalComparator)
        }

        fun sortAlbums(albums: List<Album>, option: AlbumSortOption): List<Album> = when (option) {
            AlbumSortOption.TITLE -> albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            AlbumSortOption.ARTIST -> albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
            AlbumSortOption.YEAR -> albums.sortedByDescending { it.year }
            AlbumSortOption.SONG_COUNT -> albums.sortedByDescending { it.songCount }
        }

        fun sortArtists(artists: List<Artist>, option: ArtistSortOption): List<Artist> = when (option) {
            ArtistSortOption.NAME -> artists.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            ArtistSortOption.SONG_COUNT -> artists.sortedByDescending { it.songCount }
            ArtistSortOption.ALBUM_COUNT -> artists.sortedByDescending { it.albumCount }
        }
    }
}
