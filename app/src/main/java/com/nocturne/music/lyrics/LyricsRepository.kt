package com.nocturne.music.lyrics

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.nocturne.music.database.LyricsCacheEntity
import com.nocturne.music.database.MusicDao
import com.nocturne.music.datastore.UserPreferencesRepository
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.LyricsSource
import com.nocturne.music.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val embeddedLyricsExtractor: EmbeddedLyricsExtractor,
    private val preferencesRepository: UserPreferencesRepository
) {
    val cachedLyricsCountFlow: Flow<Int> = musicDao.observeCachedLyricsCount()

    suspend fun loadLyricsForSong(
        song: Song,
        forceRefresh: Boolean = false
    ): LyricsDocument = withContext(Dispatchers.IO) {
        val settings = preferencesRepository.userSettingsFlow.first()

        // 1. Check Room offline cache first unless forceRefresh is requested
        if (!forceRefresh) {
            val cached = musicDao.getCachedLyrics(song.id)
            if (cached != null && cached.rawContent.isNotBlank()) {
                val source = runCatching { LyricsSource.valueOf(cached.sourceName) }
                    .getOrDefault(LyricsSource.LOCAL_LRC)
                return@withContext LrcParser.parse(
                    songId = song.id,
                    rawText = cached.rawContent,
                    source = source,
                    customOffsetMs = cached.offsetMs,
                    lrcFilePath = cached.lrcFilePath
                )
            }
        }

        // 2. Discover local .lrc sidecar file or embedded lyrics according to user preference
        val discoveryOrder = if (settings.preferEmbeddedLyrics) {
            listOf("EMBEDDED", "LOCAL_LRC")
        } else {
            listOf("LOCAL_LRC", "EMBEDDED")
        }

        for (step in discoveryOrder) {
            when (step) {
                "LOCAL_LRC" -> {
                    val localFile = findLocalLrcFile(song)
                    if (localFile != null && localFile.exists() && localFile.canRead()) {
                        val content = runCatching { localFile.readText(Charsets.UTF_8) }.getOrNull()
                        if (!content.isNullOrBlank()) {
                            return@withContext cacheAndParse(
                                songId = song.id,
                                rawContent = content,
                                source = LyricsSource.LOCAL_LRC,
                                lrcFilePath = localFile.absolutePath
                            )
                        }
                    }
                }
                "EMBEDDED" -> {
                    val embedded = embeddedLyricsExtractor.extractEmbeddedLyrics(
                        filePath = song.filePath,
                        contentUri = song.contentUri
                    )
                    if (!embedded.isNullOrBlank()) {
                        return@withContext cacheAndParse(
                            songId = song.id,
                            rawContent = embedded,
                            source = LyricsSource.EMBEDDED,
                            lrcFilePath = null
                        )
                    }
                }
            }
        }

        // 3. Optional online LRCLIB lookup if enabled in settings
        if (settings.autoFetchOnlineLyrics) {
            val onlineDoc = fetchOnlineLyricsAndCache(song)
            if (onlineDoc.source != LyricsSource.NONE) {
                return@withContext onlineDoc
            }
        }

        LyricsDocument(
            songId = song.id,
            isSynced = false,
            hasWordSync = false,
            source = LyricsSource.NONE
        )
    }

    suspend fun importLrcFromUri(songId: Long, documentUri: Uri): LyricsDocument = withContext(Dispatchers.IO) {
        val rawContent = context.contentResolver.openInputStream(documentUri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
        }.orEmpty()

        if (rawContent.isBlank()) {
            return@withContext LyricsDocument(
                songId = songId,
                isSynced = false,
                hasWordSync = false,
                source = LyricsSource.NONE
            )
        }

        cacheAndParse(
            songId = songId,
            rawContent = rawContent,
            source = LyricsSource.IMPORTED_LRC,
            lrcFilePath = documentUri.toString()
        )
    }

    suspend fun saveManualLyricsText(songId: Long, rawLrcOrText: String): LyricsDocument = withContext(Dispatchers.IO) {
        if (rawLrcOrText.isBlank()) {
            musicDao.deleteLyricsForSong(songId)
            return@withContext LyricsDocument(
                songId = songId,
                isSynced = false,
                hasWordSync = false,
                source = LyricsSource.NONE
            )
        }
        cacheAndParse(
            songId = songId,
            rawContent = rawLrcOrText,
            source = LyricsSource.IMPORTED_LRC,
            lrcFilePath = null
        )
    }

    suspend fun fetchOnlineLyricsAndCache(song: Song): LyricsDocument = withContext(Dispatchers.IO) {
        try {
            val trackEncoded = URLEncoder.encode(song.title, "UTF-8")
            val artistEncoded = URLEncoder.encode(song.artist, "UTF-8")
            val albumEncoded = URLEncoder.encode(song.album, "UTF-8")
            val durationSec = (song.durationMs / 1000L).coerceAtLeast(1L)
            val urlString = "https://lrclib.net/api/get?track_name=$trackEncoded&artist_name=$artistEncoded&album_name=$albumEncoded&duration=$durationSec"

            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "NocturneMusicPlayer/1.0 (Android; Offline-First Audio Player)")
            }

            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val json = JSONObject(responseBody)
                    val syncedLyrics = json.optString("syncedLyrics", "").trim()
                    val plainLyrics = json.optString("plainLyrics", "").trim()
                    val bestLyrics = syncedLyrics.ifBlank { plainLyrics }
                    if (bestLyrics.isNotBlank()) {
                        return@withContext cacheAndParse(
                            songId = song.id,
                            rawContent = bestLyrics,
                            source = LyricsSource.ONLINE_CACHE,
                            lrcFilePath = null
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Throwable) {
            // Device is offline or API unreachable; fail silently as an offline-first player
        }

        LyricsDocument(
            songId = song.id,
            isSynced = false,
            hasWordSync = false,
            source = LyricsSource.NONE
        )
    }

    suspend fun adjustLyricsOffset(songId: Long, deltaMs: Long): LyricsDocument? = withContext(Dispatchers.IO) {
        val cached = musicDao.getCachedLyrics(songId) ?: return@withContext null
        val newOffset = (cached.offsetMs + deltaMs).coerceIn(-15_000L, 15_000L)
        musicDao.updateLyricsOffset(songId, newOffset)
        val source = runCatching { LyricsSource.valueOf(cached.sourceName) }.getOrDefault(LyricsSource.LOCAL_LRC)
        LrcParser.parse(
            songId = songId,
            rawText = cached.rawContent,
            source = source,
            customOffsetMs = newOffset,
            lrcFilePath = cached.lrcFilePath
        )
    }

    suspend fun clearAllCachedLyrics() = withContext(Dispatchers.IO) {
        musicDao.clearAllLyricsCache()
    }

    private suspend fun cacheAndParse(
        songId: Long,
        rawContent: String,
        source: LyricsSource,
        lrcFilePath: String?
    ): LyricsDocument {
        val doc = LrcParser.parse(
            songId = songId,
            rawText = rawContent,
            source = source,
            lrcFilePath = lrcFilePath
        )
        musicDao.upsertLyricsCache(
            LyricsCacheEntity(
                songId = songId,
                rawContent = rawContent,
                isSynced = doc.isSynced,
                hasWordSync = doc.hasWordSync,
                sourceName = doc.source.name,
                offsetMs = doc.offsetMs,
                lrcFilePath = lrcFilePath
            )
        )
        return doc
    }

    private fun findLocalLrcFile(song: Song): File? {
        val candidates = mutableListOf<File>()
        if (song.filePath.isNotBlank()) {
            val audioFile = File(song.filePath)
            val parentDir = audioFile.parentFile
            val baseName = audioFile.nameWithoutExtension
            if (parentDir != null) {
                candidates.add(File(parentDir, "$baseName.lrc"))
                candidates.add(File(parentDir, "$baseName.LRC"))
                candidates.add(File(parentDir, "${song.artist} - ${song.title}.lrc"))
                candidates.add(File(parentDir, "${song.title}.lrc"))
                candidates.add(File(parentDir, "Lyrics/$baseName.lrc"))
                candidates.add(File(parentDir, "lyrics/$baseName.lrc"))
            }
        }
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir != null) {
            candidates.add(File(musicDir, "Lyrics/${song.artist} - ${song.title}.lrc"))
            candidates.add(File(musicDir, "Lyrics/${song.title}.lrc"))
        }
        return candidates.firstOrNull { it.exists() && it.isFile && it.canRead() }
    }
}
