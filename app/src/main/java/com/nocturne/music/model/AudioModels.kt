package com.nocturne.music.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val genre: String = "Unknown Genre",
    val durationMs: Long,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val dateAddedSec: Long = 0L,
    val dateModifiedSec: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val channels: Int = 2,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val contentUri: String,
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAtMs: Long = 0L,
    val replayGainDb: Float = 0f,
    val customArtworkUpdatedAtMs: Long = 0L
) {
    val audioQualityBadge: String
        get() {
            val ext = filePath.substringAfterLast('.', "").uppercase()
            val isLossless = ext in listOf("FLAC", "WAV", "ALAC", "APE", "WV", "DSD", "DSF") ||
                mimeType.contains("flac", ignoreCase = true) ||
                mimeType.contains("wav", ignoreCase = true)
            val formatLabel = when {
                ext.isNotBlank() && ext.length <= 5 -> ext
                mimeType.contains("flac", true) -> "FLAC"
                mimeType.contains("mp4", true) || mimeType.contains("m4a", true) -> "AAC"
                mimeType.contains("ogg", true) || mimeType.contains("opus", true) -> "OPUS"
                mimeType.contains("wav", true) -> "WAV"
                else -> "MP3"
            }
            return buildString {
                append(formatLabel)
                if (sampleRateHz > 0) {
                    val khz = sampleRateHz / 1000f
                    val formattedKhz = if (sampleRateHz % 1000 == 0) "${sampleRateHz / 1000}" else String.format("%.1f", khz)
                    append(" • ${formattedKhz}kHz")
                }
                if (bitrateKbps > 0) {
                    append(" • ${bitrateKbps}kbps")
                } else if (isLossless) {
                    append(" • Lossless")
                }
            }
        }
}

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val year: Int,
    val songCount: Int,
    val totalDurationMs: Long,
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val pinnedAtMs: Long? = null,
    val songs: List<Song> = emptyList()
)

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val totalDurationMs: Long,
    val artworkUri: String?,
    val isFavorite: Boolean = false,
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList()
)

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val songCount: Int,
    val totalDurationMs: Long,
    val coverArtUri: String?,
    val songs: List<Song> = emptyList()
)

@Immutable
data class Genre(
    val name: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val artworkUri: String?,
    val songs: List<Song> = emptyList()
)

@Immutable
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val isExcluded: Boolean = false,
    val songs: List<Song> = emptyList()
)

@Immutable
data class ListeningHistoryEntry(
    val historyId: Long,
    val song: Song,
    val playedAtMs: Long,
    val listenedDurationMs: Long
)

@Immutable
data class SearchResults(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val genres: List<Genre> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() &&
            playlists.isEmpty() && genres.isEmpty()

    val totalMatches: Int
        get() = songs.size + albums.size + artists.size + playlists.size + genres.size
}
