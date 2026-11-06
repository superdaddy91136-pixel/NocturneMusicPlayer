package com.nocturne.music.library

import android.content.Context
import android.net.Uri
import com.nocturne.music.database.*
import com.nocturne.music.datastore.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val prefs: UserPreferencesRepository
) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)

        val settings = prefs.userSettingsFlow.first()
        val settingsObj = JSONObject()
        settingsObj.put("profileName", settings.profileName)
        settingsObj.put("profilePictureUri", settings.profilePictureUri ?: "")
        settingsObj.put("gaplessPlayback", settings.gaplessPlayback)
        settingsObj.put("crossfadeDurationSec", settings.crossfadeDurationSec)
        settingsObj.put("isLibraryGridView", settings.isLibraryGridView)
        settingsObj.put("isLibraryLandingGridView", settings.isLibraryLandingGridView)
        settingsObj.put("volumeNormalization", settings.volumeNormalization)
        settingsObj.put("normalizationPreampDb", settings.normalizationPreampDb.toDouble())
        settingsObj.put("resumeOnBluetoothConnect", settings.resumeOnBluetoothConnect)
        settingsObj.put("pauseOnZeroVolume", settings.pauseOnZeroVolume)
        settingsObj.put("rememberPlaybackPosition", settings.rememberPlaybackPosition)
        settingsObj.put("defaultPlaybackSpeed", settings.defaultPlaybackSpeed.toDouble())
        settingsObj.put("autoFetchOnlineLyrics", settings.autoFetchOnlineLyrics)
        settingsObj.put("preferEmbeddedLyrics", settings.preferEmbeddedLyrics)
        settingsObj.put("wordLevelKaraokeHighlight", settings.wordLevelKaraokeHighlight)
        settingsObj.put("keepScreenOnDuringLyrics", settings.keepScreenOnDuringLyrics)
        settingsObj.put("lyricsFontSize", settings.lyricsFontSize.name)
        settingsObj.put("dynamicBackgroundStyle", settings.dynamicBackgroundStyle.name)
        settingsObj.put("highQualityArtworkCache", settings.highQualityArtworkCache)
        settingsObj.put("smoothArtworkTransitions", settings.smoothArtworkTransitions)
        settingsObj.put("reducedMotion", settings.reducedMotion)
        settingsObj.put("minimumTrackDurationSec", settings.minimumTrackDurationSec)
        settingsObj.put("ignoreRingtoneAndNotificationFolders", settings.ignoreRingtoneAndNotificationFolders)
        settingsObj.put("songSortOption", settings.songSortOption.name)
        settingsObj.put("songSortDirection", settings.songSortDirection.name)
        settingsObj.put("albumSortOption", settings.albumSortOption.name)
        settingsObj.put("artistSortOption", settings.artistSortOption.name)
        root.put("settings", settingsObj)

        val favoritesArray = JSONArray()
        musicDao.getAllSongsOnce().filter { it.isFavorite }.forEach {
            favoritesArray.put(it.id)
        }
        root.put("favoriteSongs", favoritesArray)

        val playlistsArray = JSONArray()
        musicDao.getPlaylistsOnce().forEach { p ->
            val pObj = JSONObject()
            pObj.put("name", p.name)
            pObj.put("description", p.description)
            pObj.put("createdAtMs", p.createdAtMs)
            pObj.put("updatedAtMs", p.updatedAtMs)
            val songsArr = JSONArray()
            musicDao.getPlaylistCrossRefsOnce(p.playlistId).forEach { ref ->
                val refObj = JSONObject()
                refObj.put("songId", ref.songId)
                refObj.put("position", ref.position)
                songsArr.put(refObj)
            }
            pObj.put("songs", songsArr)
            playlistsArray.put(pObj)
        }
        root.put("playlists", playlistsArray)

        root.toString()
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext false

            val root = JSONObject(jsonString)
            if (!root.has("version")) return@withContext false

            if (root.has("settings")) {
                val settingsObj = root.getJSONObject("settings")
                if (settingsObj.has("profileName")) {
                    prefs.setProfileInfo(
                        name = settingsObj.getString("profileName"),
                        pictureUri = settingsObj.getString("profilePictureUri").takeIf { it.isNotBlank() }
                    )
                }
            }
            
            if (root.has("favoriteSongs")) {
                val favs = root.getJSONArray("favoriteSongs")
                for (i in 0 until favs.length()) {
                    val id = favs.getLong(i)
                    musicDao.updateSongFavorite(id, true)
                }
            }
            
            if (root.has("playlists")) {
                val playlistsArr = root.getJSONArray("playlists")
                for (i in 0 until playlistsArr.length()) {
                    val pObj = playlistsArr.getJSONObject(i)
                    val pId = musicDao.insertPlaylist(PlaylistEntity(
                        name = pObj.getString("name"),
                        description = pObj.getString("description"),
                        createdAtMs = pObj.getLong("createdAtMs"),
                        updatedAtMs = pObj.getLong("updatedAtMs")
                    ))
                    
                    val songsArr = pObj.getJSONArray("songs")
                    val crossRefs = mutableListOf<PlaylistSongCrossRef>()
                    for (j in 0 until songsArr.length()) {
                        val refObj = songsArr.getJSONObject(j)
                        crossRefs.add(PlaylistSongCrossRef(
                            playlistId = pId,
                            songId = refObj.getLong("songId"),
                            position = refObj.getInt("position")
                        ))
                    }
                    musicDao.insertPlaylistSongCrossRefs(crossRefs)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
