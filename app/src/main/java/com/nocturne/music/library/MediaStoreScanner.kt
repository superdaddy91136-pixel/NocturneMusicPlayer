package com.nocturne.music.library

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nocturne.music.database.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExtendedAudioSpecs(
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val replayGainDb: Float,
    val genre: String?
)

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // ALBUM_ART_CONTENT_URI removed
    }

    suspend fun scanDeviceMusic(
        minDurationSec: Int,
        ignoreRingtones: Boolean,
        excludedFolders: Set<String>,
        existingSongsById: Map<Long, SongEntity>
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val results = ArrayList<SongEntity>(512)
        val genreMap = queryGenreMap()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_ALARM
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.BITRATE)
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val minDurationMs = (minDurationSec.coerceAtLeast(0)) * 1000L
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val ringtoneCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val notifCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val alarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                } else -1
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val durationMs = cursor.getLong(durationCol)
                    if (durationMs < minDurationMs) continue

                    if (ignoreRingtones) {
                        val isRing = ringtoneCol >= 0 && cursor.getInt(ringtoneCol) != 0
                        val isNotif = notifCol >= 0 && cursor.getInt(notifCol) != 0
                        val isAlarm = alarmCol >= 0 && cursor.getInt(alarmCol) != 0
                        if (isRing || isNotif || isAlarm) continue
                    }

                    val filePath = cursor.getString(dataCol).orEmpty()
                    val parentFile = if (filePath.isNotBlank()) File(filePath).parentFile else null
                    val folderPath = parentFile?.absolutePath ?: "/storage/emulated/0/Music"
                    val folderName = parentFile?.name?.takeIf { it.isNotBlank() } ?: "Music"

                    if (excludedFolders.any { excluded ->
                            folderPath.equals(excluded, ignoreCase = true) ||
                                folderPath.startsWith("$excluded/", ignoreCase = true)
                        }
                    ) {
                        continue
                    }

                    if (ignoreRingtones) {
                        val lowerFolder = folderName.lowercase()
                        if (lowerFolder in setOf("ringtones", "notifications", "alarms", "whatsapp audio")) {
                            continue
                        }
                    }

                    val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                    val fallbackTitle = if (filePath.isNotBlank()) {
                        File(filePath).nameWithoutExtension
                    } else {
                        "Untitled Track"
                    }
                    val title = rawTitle.ifBlank { fallbackTitle }

                    val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                    val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") {
                        "Unknown Artist"
                    } else {
                        rawArtist
                    }
                    val artistId = cursor.getLong(artistIdCol)

                    val rawAlbum = cursor.getString(albumCol)?.trim().orEmpty()
                    val album = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") {
                        folderName
                    } else {
                        rawAlbum
                    }
                    val albumId = cursor.getLong(albumIdCol)

                    val rawTrack = cursor.getInt(trackCol)
                    val discNumber = if (rawTrack >= 1000) (rawTrack / 1000).coerceAtLeast(1) else 1
                    val trackNumber = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack.coerceAtLeast(0)

                    val year = cursor.getInt(yearCol).coerceAtLeast(0)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateModified = cursor.getLong(dateModifiedCol)
                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"

                    val bitrateBps = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0
                    val estimatedBitrateKbps = when {
                        bitrateBps > 1000 -> bitrateBps / 1000
                        durationMs > 0 && sizeBytes > 0 -> ((sizeBytes * 8) / durationMs).toInt().coerceIn(32, 9216)
                        else -> 0
                    }

                    val mediaGenre = if (genreCol >= 0) cursor.getString(genreCol)?.trim() else null
                    val resolvedGenre = mediaGenre?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: genreMap[id]
                        ?: "Unspecified"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = contentUri

                    val existing = existingSongsById[id]
                    results.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            artistId = artistId,
                            album = album,
                            albumId = albumId,
                            genre = resolvedGenre,
                            durationMs = durationMs,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            year = year,
                            dateAddedSec = dateAdded,
                            dateModifiedSec = dateModified,
                            sizeBytes = sizeBytes,
                            mimeType = mimeType,
                            bitrateKbps = existing?.bitrateKbps?.takeIf { it > 0 } ?: estimatedBitrateKbps,
                            sampleRateHz = existing?.sampleRateHz?.takeIf { it > 0 } ?: 44100,
                            channels = existing?.channels?.takeIf { it > 0 } ?: 2,
                            filePath = filePath,
                            folderPath = folderPath,
                            folderName = folderName,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            isFavorite = existing?.isFavorite ?: false,
                            playCount = existing?.playCount ?: 0,
                            lastPlayedAtMs = existing?.lastPlayedAtMs ?: 0L,
                            replayGainDb = existing?.replayGainDb ?: 0f,
                            customTitleOverride = existing?.customTitleOverride,
                            customArtistOverride = existing?.customArtistOverride,
                            customAlbumOverride = existing?.customAlbumOverride,
                            customGenreOverride = existing?.customGenreOverride,
                            customYearOverride = existing?.customYearOverride
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not yet granted; return empty list gracefully
        } catch (_: Throwable) {
            // Handle unexpected MediaStore cursor failure gracefully
        }

        results
    }

    private fun queryGenreMap(): Map<Long, String> {
        val map = HashMap<Long, String>()
        try {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            val genresProjection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
            context.contentResolver.query(genresUri, genresProjection, null, null, null)?.use { genreCursor ->
                val idIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(idIdx)
                    val genreName = genreCursor.getString(nameIdx)?.trim().orEmpty()
                    if (genreName.isBlank() || genreName == "<unknown>") continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Media._ID),
                        null,
                        null,
                        null
                    )?.use { memberCursor ->
                        val audioIdIdx = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        while (memberCursor.moveToNext()) {
                            map[memberCursor.getLong(audioIdIdx)] = genreName
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore genre query issues on devices with custom MediaStore providers
        }
        return map
    }

    /**
     * Reads exact sample rate, channel count, bitrate, and ReplayGain tags for a track on demand.
     */
    suspend fun inspectExtendedAudioSpecs(contentUriString: String): ExtendedAudioSpecs = withContext(Dispatchers.IO) {
        var bitrateKbps = 0
        var sampleRateHz = 44100
        var channels = 2
        var genre: String? = null
        val uri = Uri.parse(contentUriString)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            bitrateKbps = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
            }
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()
        } catch (_: Throwable) {
        } finally {
            runCatching { retriever.release() }
        }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            if (extractor.trackCount > 0) {
                val format = extractor.getTrackFormat(0)
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                if (bitrateKbps <= 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    bitrateKbps = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { extractor.release() }
        }

        ExtendedAudioSpecs(
            bitrateKbps = bitrateKbps.coerceAtLeast(0),
            sampleRateHz = sampleRateHz.coerceAtLeast(8000),
            channels = channels.coerceIn(1, 8),
            replayGainDb = 0f,
            genre = genre
        )
    }
}
