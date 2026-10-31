package com.nocturne.music.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // Songs
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsOnce(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<Long>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAtMs > 0 ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    fun observeRecentlyPlayedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAddedSec DESC LIMIT :limit")
    fun observeRecentlyAddedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAtMs DESC LIMIT :limit")
    fun observeMostPlayedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:validIds)")
    suspend fun deleteSongsNotIn(validIds: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateSongFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAtMs = :timestampMs WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, timestampMs: Long)

    @Query("UPDATE songs SET customTitleOverride = :title, customArtistOverride = :artist, customAlbumOverride = :album, customGenreOverride = :genre, customYearOverride = :year WHERE id = :songId")
    suspend fun updateSongMetadataOverrides(
        songId: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?
    )

    @Query("UPDATE songs SET bitrateKbps = :bitrateKbps, sampleRateHz = :sampleRateHz, channels = :channels, replayGainDb = :replayGainDb WHERE id = :songId")
    suspend fun updateExtendedAudioSpecs(
        songId: Long,
        bitrateKbps: Int,
        sampleRateHz: Int,
        channels: Int,
        replayGainDb: Float
    )

    // Favorite Albums & Artists
    @Query("SELECT albumId FROM favorite_albums")
    fun observeFavoriteAlbumIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteAlbum(entity: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_albums WHERE albumId = :albumId")
    suspend fun removeFavoriteAlbum(albumId: Long)

    @Query("SELECT artistId FROM favorite_artists")
    fun observeFavoriteArtistIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteArtist(entity: FavoriteArtistEntity)

    @Query("DELETE FROM favorite_artists WHERE artistId = :artistId")
    suspend fun removeFavoriteArtist(artistId: Long)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY updatedAtMs DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId ASC, position ASC")
    fun observeAllPlaylistCrossRefs(): Flow<List<PlaylistSongCrossRef>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistCrossRefsOnce(playlistId: Long): List<PlaylistSongCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, description = :description, updatedAtMs = :updatedAtMs WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String, description: String, updatedAtMs: Long)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPositionInPlaylist(playlistId: Long): Int

    // Play History
    @Query("SELECT * FROM play_history ORDER BY playedAtMs DESC LIMIT :limit")
    fun observePlayHistory(limit: Int = 100): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(entry: PlayHistoryEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearPlayHistory()

    // Lyrics Cache
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    suspend fun getCachedLyrics(songId: Long): LyricsCacheEntity?

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    fun observeCachedLyrics(songId: Long): Flow<LyricsCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyricsCache(entity: LyricsCacheEntity)

    @Query("UPDATE lyrics_cache SET offsetMs = :offsetMs, updatedAtMs = :updatedAtMs WHERE songId = :songId")
    suspend fun updateLyricsOffset(songId: Long, offsetMs: Long, updatedAtMs: Long = System.currentTimeMillis())

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun deleteLyricsForSong(songId: Long)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAllLyricsCache()

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    fun observeCachedLyricsCount(): Flow<Int>

    // Search History
    @Query("SELECT * FROM search_history ORDER BY searchedAtMs DESC LIMIT 20")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchQuery(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Excluded Folders
    @Query("SELECT * FROM excluded_folders ORDER BY addedAtMs DESC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT folderPath FROM excluded_folders")
    suspend fun getExcludedFolderPathsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addExcludedFolder(entity: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun removeExcludedFolder(folderPath: String)

    // Custom Artwork
    @Query("SELECT * FROM custom_artwork")
    fun observeAllCustomArtwork(): Flow<List<CustomArtworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomArtwork(entity: CustomArtworkEntity)

    @Query("DELETE FROM custom_artwork WHERE songId = :songId")
    suspend fun deleteCustomArtwork(songId: Long)
}
