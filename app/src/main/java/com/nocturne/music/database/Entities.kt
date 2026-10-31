package com.nocturne.music.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.nocturne.music.model.LyricsSource
import com.nocturne.music.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
        Index(value = ["folderPath"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedAtMs"]),
        Index(value = ["playCount"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val genre: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val contentUri: String,
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAtMs: Long = 0L,
    val replayGainDb: Float = 0f,
    val customTitleOverride: String? = null,
    val customArtistOverride: String? = null,
    val customAlbumOverride: String? = null,
    val customGenreOverride: String? = null,
    val customYearOverride: Int? = null,
    val customArtworkUpdatedAtMs: Long = 0L
) {
    fun toDomainModel(): Song = Song(
        id = id,
        title = customTitleOverride?.takeIf { it.isNotBlank() } ?: title,
        artist = customArtistOverride?.takeIf { it.isNotBlank() } ?: artist,
        artistId = artistId,
        album = customAlbumOverride?.takeIf { it.isNotBlank() } ?: album,
        albumId = albumId,
        genre = customGenreOverride?.takeIf { it.isNotBlank() } ?: genre,
        durationMs = durationMs,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = customYearOverride ?: year,
        dateAddedSec = dateAddedSec,
        dateModifiedSec = dateModifiedSec,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        bitrateKbps = bitrateKbps,
        sampleRateHz = sampleRateHz,
        channels = channels,
        filePath = filePath,
        folderPath = folderPath,
        folderName = folderName,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAtMs = lastPlayedAtMs,
        replayGainDb = replayGainDb,
        customArtworkUpdatedAtMs = customArtworkUpdatedAtMs
    )
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val customCoverUri: String? = null
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["songId"]), Index(value = ["playlistId", "position"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAtMs: Long = System.currentTimeMillis()
)

data class PlaylistWithSongsRelation(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

@Entity(
    tableName = "play_history",
    indices = [Index(value = ["playedAtMs"]), Index(value = ["songId"])]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0L,
    val songId: Long,
    val playedAtMs: Long = System.currentTimeMillis(),
    val listenedDurationMs: Long = 0L
)

@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val albumId: Long,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_artists")
data class FavoriteArtistEntity(
    @PrimaryKey val artistId: Long,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val songId: Long,
    val rawContent: String,
    val isSynced: Boolean,
    val hasWordSync: Boolean,
    val sourceName: String = LyricsSource.LOCAL_LRC.name,
    val offsetMs: Long = 0L,
    val lrcFilePath: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_artwork")
data class CustomArtworkEntity(
    @PrimaryKey val songId: Long,
    val artworkPath: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)
