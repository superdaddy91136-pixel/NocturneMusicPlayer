package com.nocturne.music.playback

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nocturne.music.datastore.UserPreferencesRepository
import com.nocturne.music.library.LibraryRepository
import com.nocturne.music.model.PlaybackUiState
import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.SleepTimerState
import com.nocturne.music.model.Song
import com.nocturne.music.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val audioEffectsController: AudioEffectsController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queueManager = QueueManager()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private var progressPollingJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var saveSessionJob: Job? = null
    private var latestSettings: UserSettings = UserSettings()
    private var hasRestoredSession: Boolean = false
    private var lastRecordedPlaySongId: Long = -1L

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    init {
        connectToService()
        observeSettingsAndEqualizer()
    }

    private fun connectToService() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    runCatching {
                        val controller = future.get()
                        mediaController = controller
                        attachPlayerListener(controller)
                        scope.launch { restorePreviousSessionIfNeeded() }
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        } catch (_: Throwable) {
        }
    }

    private fun observeSettingsAndEqualizer() {
        scope.launch {
            preferencesRepository.userSettingsFlow.collectLatest { settings ->
                latestSettings = settings
                val currentSong = _playbackState.value.currentSong
                audioEffectsController.configureVolumeNormalization(
                    enabled = settings.volumeNormalization,
                    preampDb = settings.normalizationPreampDb,
                    currentTrackReplayGainDb = currentSong?.replayGainDb ?: 0f
                )
            }
        }

        scope.launch {
            preferencesRepository.equalizerStateFlow.collectLatest { eqState ->
                audioEffectsController.applyEqualizerState(eqState)
                _playbackState.update { it.copy(equalizerState = eqState) }
            }
        }
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        positionMs = controller.currentPosition.coerceAtLeast(0L),
                        durationMs = controller.duration.takeIf { d -> d > 0 } ?: it.currentSong?.durationMs ?: 0L
                    )
                }
                if (isPlaying) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                    persistSessionAsync()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                _playbackState.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = controller.duration.takeIf { d -> d > 0 } ?: it.currentSong?.durationMs ?: 0L
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (_playbackState.value.sleepTimer.stopAtEndOfTrack && _playbackState.value.sleepTimer.isActive) {
                        cancelSleepTimer()
                        pause()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newIndex = controller.currentMediaItemIndex
                if (newIndex in queueManager.currentQueue.indices) {
                    val song = queueManager.selectIndex(newIndex)
                    val sleep = _playbackState.value.sleepTimer
                    if (sleep.isActive && sleep.stopAtEndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        cancelSleepTimer()
                        controller.pause()
                    }
                    _playbackState.update {
                        it.copy(
                            currentSong = song,
                            currentQueueIndex = newIndex,
                            positionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = song?.durationMs ?: controller.duration.coerceAtLeast(0L)
                        )
                    }
                    if (song != null && song.id != lastRecordedPlaySongId) {
                        lastRecordedPlaySongId = song.id
                        scope.launch {
                            libraryRepository.recordSongPlayed(song.id, song.durationMs)
                            libraryRepository.enrichSongAudioSpecs(song)
                        }
                        audioEffectsController.configureVolumeNormalization(
                            enabled = latestSettings.volumeNormalization,
                            preampDb = latestSettings.normalizationPreampDb,
                            currentTrackReplayGainDb = song.replayGainDb
                        )
                    }
                    if (song != null) {
                        updateActiveItemArtworkAsync(song)
                    }
                    persistSessionAsync()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
                stopProgressPolling()
            }
        })
    }

    suspend fun restorePreviousSessionIfNeeded() {
        val controller = mediaController
        if (hasRestoredSession && (controller == null || controller.mediaItemCount > 0)) return

        val saved = preferencesRepository.getPersistedPlaybackSession()
        
        // Restore preferences unconditionally
        val isQueueEmptyBeforeRestore = queueManager.currentQueue.isEmpty()
        if (isQueueEmptyBeforeRestore) {
            queueManager.setShuffle(saved.shuffleEnabled)
            queueManager.setRepeatMode(saved.repeatMode)
        }
        
        _playbackState.update {
            it.copy(
                shuffleEnabled = queueManager.shuffleEnabled,
                repeatMode = queueManager.repeatMode,
                playbackSpeed = saved.playbackSpeed,
                playbackPitch = saved.playbackPitch
            )
        }
        
        if (controller != null) {
            controller.repeatMode = queueManager.repeatMode.toMedia3RepeatMode()
            controller.playbackParameters = PlaybackParameters(saved.playbackSpeed, saved.playbackPitch)
        }

        if (queueManager.currentQueue.isEmpty()) {
            if (saved.queueSongIds.isEmpty()) return
            val songs = libraryRepository.getSongsByIdsOrdered(saved.queueSongIds)
            if (songs.isEmpty()) return

            val validIndex = saved.currentIndex.coerceIn(0, songs.lastIndex)
            queueManager.setQueue(songs, validIndex, shuffle = saved.shuffleEnabled)
        }

        val currentSong = queueManager.currentSong
        val restoredPos = if (latestSettings.rememberPlaybackPosition) saved.positionMs else 0L

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                queue = queueManager.currentQueue,
                currentQueueIndex = queueManager.currentIndex,
                positionMs = restoredPos,
                durationMs = currentSong?.durationMs ?: 0L
            )
        }

        if (controller != null) {
            hasRestoredSession = true
            val mediaItems = queueManager.currentQueue.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, queueManager.currentIndex, restoredPos)
            controller.prepare()
            queueManager.currentSong?.let { updateActiveItemArtworkAsync(it) }
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val effectiveShuffle = if (shuffle) true else queueManager.shuffleEnabled
        queueManager.setQueue(songs, startIndex, shuffle = effectiveShuffle)
        val activeSongs = queueManager.currentQueue
        val activeIndex = queueManager.currentIndex
        val current = queueManager.currentSong ?: return

        _playbackState.update {
            it.copy(
                currentSong = current,
                queue = activeSongs,
                currentQueueIndex = activeIndex,
                isPlaying = true,
                positionMs = 0L,
                durationMs = current.durationMs,
                shuffleEnabled = queueManager.shuffleEnabled
            )
        }

        lastRecordedPlaySongId = current.id
        scope.launch {
            libraryRepository.recordSongPlayed(current.id, current.durationMs)
            libraryRepository.enrichSongAudioSpecs(current)
        }

        val controller = mediaController ?: return
        val items = activeSongs.map { it.toMediaItem() }
        controller.setMediaItems(items, activeIndex, 0L)
        controller.repeatMode = queueManager.repeatMode.toMedia3RepeatMode()
        controller.volume = 1.0f
        controller.prepare()
        controller.play()
        startProgressPolling()
        persistSessionAsync()
        updateActiveItemArtworkAsync(current)
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null) {
            _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0 && queueManager.currentQueue.isNotEmpty()) {
                val mediaItems = queueManager.currentQueue.map { it.toMediaItem() }
                val restoredPos = _playbackState.value.positionMs
                controller.setMediaItems(mediaItems, queueManager.currentIndex, restoredPos)
                controller.prepare()
            } else if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.volume = 1.0f
            controller.play()
        }
    }

    fun pause() {
        mediaController?.pause()
        _playbackState.update { it.copy(isPlaying = false) }
    }

    fun seekTo(positionMs: Long) {
        val duration = _playbackState.value.durationMs.coerceAtLeast(1L)
        val target = positionMs.coerceIn(0L, duration)
        mediaController?.seekTo(target)
        _playbackState.update { it.copy(positionMs = target) }
        persistSessionAsync()
    }

    fun skipToNext() {
        val nextIndex = queueManager.resolveNextIndex(fromAutoCompletion = false)
        if (nextIndex < 0) return
        selectQueueItem(nextIndex)
    }

    fun skipToPrevious() {
        val pos = _playbackState.value.positionMs
        if (pos > 3500L) {
            seekTo(0L)
            return
        }
        val prevIndex = queueManager.resolvePreviousIndex(pos)
        if (prevIndex >= 0) {
            selectQueueItem(prevIndex)
        }
    }

    fun selectQueueItem(index: Int) {
        val song = queueManager.selectIndex(index) ?: return
        _playbackState.update {
            it.copy(
                currentSong = song,
                currentQueueIndex = index,
                positionMs = 0L,
                durationMs = song.durationMs,
                isPlaying = true
            )
        }
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
        } else {
            controller.setMediaItems(queueManager.currentQueue.map { it.toMediaItem() }, index, 0L)
            controller.prepare()
            controller.play()
        }
        persistSessionAsync()
    }

    fun toggleShuffle() {
        val newShuffle = !queueManager.shuffleEnabled
        val newIndex = queueManager.setShuffle(newShuffle)
        val newQueue = queueManager.currentQueue
        val pos = mediaController?.currentPosition ?: _playbackState.value.positionMs

        _playbackState.update {
            it.copy(
                shuffleEnabled = newShuffle,
                queue = newQueue,
                currentQueueIndex = newIndex
            )
        }

        val controller = mediaController ?: return
        if (newQueue.isNotEmpty() && newIndex >= 0) {
            val wasPlaying = controller.isPlaying
            controller.setMediaItems(newQueue.map { it.toMediaItem() }, newIndex, pos)
            controller.prepare()
            if (wasPlaying) controller.play()
        }
        persistSessionAsync()
    }

    fun cycleRepeatMode() {
        val nextMode = queueManager.cycleRepeatMode()
        mediaController?.repeatMode = nextMode.toMedia3RepeatMode()
        _playbackState.update { it.copy(repeatMode = nextMode) }
        persistSessionAsync()
    }

    fun setPlaybackMode(shuffle: Boolean, repeatMode: RepeatModeState) {
        val oldShuffle = queueManager.shuffleEnabled
        val oldRepeat = queueManager.repeatMode

        if (oldShuffle != shuffle) {
            queueManager.setShuffle(shuffle)
            val newQueue = queueManager.currentQueue
            val newIndex = queueManager.currentIndex
            val pos = mediaController?.currentPosition ?: _playbackState.value.positionMs

            _playbackState.update {
                it.copy(
                    shuffleEnabled = shuffle,
                    queue = newQueue,
                    currentQueueIndex = newIndex
                )
            }

            val controller = mediaController
            if (controller != null && newQueue.isNotEmpty() && newIndex >= 0) {
                val wasPlaying = controller.isPlaying
                controller.setMediaItems(newQueue.map { it.toMediaItem() }, newIndex, pos)
                controller.prepare()
                if (wasPlaying) controller.play()
            }
        }

        if (oldRepeat != repeatMode) {
            queueManager.setRepeatMode(repeatMode)
            mediaController?.repeatMode = repeatMode.toMedia3RepeatMode()
            _playbackState.update { it.copy(repeatMode = repeatMode) }
        }

        persistSessionAsync()
    }

    fun playNext(song: Song) {
        val wasEmpty = queueManager.currentQueue.isEmpty()
        val insertIdx = queueManager.playNext(song)
        _playbackState.update {
            it.copy(
                currentSong = queueManager.currentSong,
                queue = queueManager.currentQueue,
                currentQueueIndex = queueManager.currentIndex
            )
        }
        val controller = mediaController ?: return
        if (wasEmpty) {
            playSongs(listOf(song), 0)
        } else {
            controller.addMediaItem(insertIdx.coerceIn(0, controller.mediaItemCount), song.toMediaItem())
        }
        persistSessionAsync()
    }

    fun addToQueue(song: Song) {
        val wasEmpty = queueManager.currentQueue.isEmpty()
        queueManager.addToQueue(song)
        _playbackState.update {
            it.copy(
                currentSong = queueManager.currentSong,
                queue = queueManager.currentQueue,
                currentQueueIndex = queueManager.currentIndex
            )
        }
        val controller = mediaController ?: return
        if (wasEmpty) {
            playSongs(listOf(song), 0)
        } else {
            controller.addMediaItem(song.toMediaItem())
        }
        persistSessionAsync()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (!queueManager.moveQueueItem(fromIndex, toIndex)) return
        _playbackState.update {
            it.copy(
                queue = queueManager.currentQueue,
                currentQueueIndex = queueManager.currentIndex
            )
        }
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
        }
        persistSessionAsync()
    }

    fun removeQueueItem(index: Int) {
        val removingCurrent = index == queueManager.currentIndex
        if (!queueManager.removeQueueItem(index)) return
        _playbackState.update {
            it.copy(
                currentSong = queueManager.currentSong,
                queue = queueManager.currentQueue,
                currentQueueIndex = queueManager.currentIndex,
                isPlaying = if (queueManager.currentQueue.isEmpty()) false else it.isPlaying
            )
        }
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
        }
        if (removingCurrent && queueManager.currentQueue.isEmpty()) {
            controller.stop()
        }
        persistSessionAsync()
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float = _playbackState.value.playbackPitch) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        mediaController?.playbackParameters = PlaybackParameters(safeSpeed, safePitch)
        _playbackState.update {
            it.copy(playbackSpeed = safeSpeed, playbackPitch = safePitch)
        }
        persistSessionAsync()
    }

    fun startSleepTimer(durationMinutes: Int, stopAtEndOfTrack: Boolean = false, fadeOut: Boolean = true) {
        sleepTimerJob?.cancel()
        if (stopAtEndOfTrack && durationMinutes <= 0) {
            _playbackState.update {
                it.copy(
                    sleepTimer = SleepTimerState(
                        isActive = true,
                        remainingMs = (_playbackState.value.durationMs - _playbackState.value.positionMs).coerceAtLeast(0L),
                        totalDurationMinutes = 0,
                        stopAtEndOfTrack = true,
                        fadeOutEnabled = fadeOut
                    )
                )
            }
            return
        }

        val totalMs = durationMinutes.coerceAtLeast(1) * 60_000L
        _playbackState.update {
            it.copy(
                sleepTimer = SleepTimerState(
                    isActive = true,
                    remainingMs = totalMs,
                    totalDurationMinutes = durationMinutes,
                    stopAtEndOfTrack = stopAtEndOfTrack,
                    fadeOutEnabled = fadeOut
                )
            )
        }

        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0L) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                _playbackState.update { state ->
                    state.copy(sleepTimer = state.sleepTimer.copy(remainingMs = remaining))
                }
                if (fadeOut && remaining in 1L..10_000L) {
                    val fadeVol = (remaining / 10_000f).coerceIn(0.05f, 1.0f)
                    mediaController?.volume = fadeVol
                }
            }
            if (stopAtEndOfTrack) {
                _playbackState.update { state ->
                    state.copy(sleepTimer = state.sleepTimer.copy(remainingMs = 0L))
                }
            } else {
                mediaController?.pause()
                mediaController?.volume = 1.0f
                cancelSleepTimer()
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        mediaController?.volume = 1.0f
        _playbackState.update {
            it.copy(sleepTimer = SleepTimerState(isActive = false))
        }
    }

    fun refreshCurrentSongFavoriteState(songId: Long, isFavorite: Boolean) {
        _playbackState.update { state ->
            val updatedCurrent = if (state.currentSong?.id == songId) {
                state.currentSong.copy(isFavorite = isFavorite)
            } else state.currentSong
            val updatedQueue = state.queue.map { if (it.id == songId) it.copy(isFavorite = isFavorite) else it }
            state.copy(currentSong = updatedCurrent, queue = updatedQueue)
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = scope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 }
                        ?: _playbackState.value.currentSong?.durationMs
                        ?: 0L

                    // Configurable Crossfade volume envelope near track boundaries
                    val crossfadeSec = latestSettings.crossfadeDurationSec
                    val crossfadeMs = crossfadeSec * 1000L
                    var crossfadingNow = false
                    if (crossfadeMs > 0L && dur > crossfadeMs * 2 && !_playbackState.value.sleepTimer.isActive) {
                        val remaining = (dur - pos).coerceAtLeast(0L)
                        when {
                            remaining in 1L..crossfadeMs -> {
                                crossfadingNow = true
                                controller.volume = (remaining.toFloat() / crossfadeMs.toFloat()).coerceIn(0.15f, 1.0f)
                            }
                            pos in 0L..minOf(crossfadeMs, 3000L) -> {
                                crossfadingNow = true
                                controller.volume = (pos.toFloat() / minOf(crossfadeMs, 3000L).toFloat()).coerceIn(0.20f, 1.0f)
                            }
                            else -> {
                                if (controller.volume < 0.99f) controller.volume = 1.0f
                            }
                        }
                    }

                    _playbackState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            isCrossfading = crossfadingNow
                        )
                    }
                }
                delay(50L) // 20Hz smooth polling for word-level karaoke & scrub bar
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    private fun persistSessionAsync() {
        saveSessionJob?.cancel()
        saveSessionJob = scope.launch(Dispatchers.IO) {
            delay(350L)
            val state = _playbackState.value
            if (state.queue.isNotEmpty()) {
                preferencesRepository.savePlaybackSession(
                    queueSongIds = state.queue.map { it.id },
                    currentIndex = state.currentQueueIndex,
                    positionMs = state.positionMs,
                    shuffleEnabled = state.shuffleEnabled,
                    repeatMode = state.repeatMode,
                    playbackSpeed = state.playbackSpeed,
                    playbackPitch = state.playbackPitch
                )
            }
        }
    }

    private fun updateActiveItemArtworkAsync(song: Song) {
        scope.launch(Dispatchers.IO) {
            val artUri = getOrCreateNotificationArtworkUri(song)
            withContext(Dispatchers.Main) {
                val controller = mediaController ?: return@withContext
                val currentIndex = controller.currentMediaItemIndex
                if (currentIndex in 0 until controller.mediaItemCount) {
                    val currentItem = controller.getMediaItemAt(currentIndex)
                    if (currentItem.mediaId == song.id.toString()) {
                        val currentMeta = currentItem.mediaMetadata
                        
                        // Break infinite onMediaItemTransition loop
                        if (currentMeta.artworkUri == artUri) return@withContext
                        
                        val newMeta = currentMeta.buildUpon()
                            .setArtworkUri(artUri)
                            .build()
                        val updatedItem = currentItem.buildUpon()
                            .setMediaMetadata(newMeta)
                            .build()
                        controller.replaceMediaItem(currentIndex, updatedItem)
                    }
                }
            }
        }
    }

    private fun getOrCreateNotificationArtworkUri(song: Song): Uri? {
        try {
            if (!song.albumArtUri.isNullOrBlank()) {
                if (song.albumArtUri.startsWith("file://") || song.albumArtUri.startsWith("/")) {
                    val file = File(song.albumArtUri.removePrefix("file://"))
                    if (file.exists()) return Uri.fromFile(file)
                }
            }

            val artCacheDir = File(context.cacheDir, "notif_artwork").apply { if (!exists()) mkdirs() }
            val artFile = File(artCacheDir, "art_${song.id}.jpg")
            if (artFile.exists() && artFile.length() > 0) {
                return Uri.fromFile(artFile)
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(song.contentUri))
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    FileOutputStream(artFile).use { fos ->
                        fos.write(picture)
                    }
                    if (artFile.exists() && artFile.length() > 0) {
                        return Uri.fromFile(artFile)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { retriever.release() }
            }
        } catch (_: Throwable) {
        }
        return null
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setGenre(genre)
            .setTrackNumber(trackNumber)
            .setDiscNumber(discNumber)
            .setReleaseYear(year.takeIf { it > 0 })

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun RepeatModeState.toMedia3RepeatMode(): Int = when (this) {
        RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
        RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
        RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
    }
}
