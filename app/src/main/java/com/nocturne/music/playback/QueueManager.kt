package com.nocturne.music.playback

import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.Song
import kotlin.random.Random

/**
 * Deterministic queue state engine handling normal & shuffled playback order,
 * reordering, item removal, play-next insertion, and repeat transitions.
 */
class QueueManager(
    private val random: Random = Random.Default
) {
    private val originalList = mutableListOf<Song>()
    private val activeQueue = mutableListOf<Song>()

    var currentIndex: Int = -1
        private set

    var shuffleEnabled: Boolean = false
        private set

    var repeatMode: RepeatModeState = RepeatModeState.OFF
        private set

    val currentQueue: List<Song>
        get() = activeQueue.toList()

    val currentSong: Song?
        get() = activeQueue.getOrNull(currentIndex)

    fun setQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = shuffleEnabled) {
        originalList.clear()
        originalList.addAll(songs)
        shuffleEnabled = shuffle

        if (songs.isEmpty()) {
            activeQueue.clear()
            currentIndex = -1
            return
        }

        val clampedStart = startIndex.coerceIn(0, songs.lastIndex)
        val anchorSong = songs[clampedStart]

        activeQueue.clear()
        if (shuffle) {
            val remaining = songs.toMutableList().apply { removeAt(clampedStart) }
            remaining.shuffle(random)
            activeQueue.add(anchorSong)
            activeQueue.addAll(remaining)
            currentIndex = 0
        } else {
            activeQueue.addAll(songs)
            currentIndex = clampedStart
        }
    }

    fun setShuffle(enabled: Boolean): Int {
        if (shuffleEnabled == enabled) return currentIndex
        shuffleEnabled = enabled
        val anchor = currentSong ?: return -1

        if (enabled) {
            val rest = activeQueue.filterIndexed { idx, _ -> idx != currentIndex }.shuffled(random)
            activeQueue.clear()
            activeQueue.add(anchor)
            activeQueue.addAll(rest)
            currentIndex = 0
        } else {
            activeQueue.clear()
            activeQueue.addAll(originalList)
            currentIndex = activeQueue.indexOfFirst { it.id == anchor.id }.coerceAtLeast(0)
        }
        return currentIndex
    }

    fun setRepeatMode(mode: RepeatModeState) {
        repeatMode = mode
    }

    fun cycleRepeatMode(): RepeatModeState {
        repeatMode = when (repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        return repeatMode
    }

    fun selectIndex(index: Int): Song? {
        if (index !in activeQueue.indices) return null
        currentIndex = index
        return activeQueue[currentIndex]
    }

    fun playNext(song: Song): Int {
        if (activeQueue.isEmpty()) {
            setQueue(listOf(song), 0, shuffle = false)
            return 0
        }
        val insertIndex = (currentIndex + 1).coerceIn(0, activeQueue.size)
        activeQueue.add(insertIndex, song)
        originalList.add(song)
        return insertIndex
    }

    fun addToQueue(song: Song): Int {
        if (activeQueue.isEmpty()) {
            setQueue(listOf(song), 0, shuffle = false)
            return 0
        }
        activeQueue.add(song)
        originalList.add(song)
        return activeQueue.lastIndex
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in activeQueue.indices || toIndex !in activeQueue.indices || fromIndex == toIndex) {
            return false
        }
        val item = activeQueue.removeAt(fromIndex)
        activeQueue.add(toIndex, item)
        when {
            currentIndex == fromIndex -> currentIndex = toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex -= 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex += 1
        }
        return true
    }

    fun removeQueueItem(index: Int): Boolean {
        if (index !in activeQueue.indices) return false
        val removed = activeQueue.removeAt(index)
        originalList.removeFirstOrNull { it.id == removed.id }

        if (activeQueue.isEmpty()) {
            currentIndex = -1
        } else if (index < currentIndex) {
            currentIndex -= 1
        } else if (currentIndex >= activeQueue.size) {
            currentIndex = activeQueue.lastIndex
        }
        return true
    }

    fun resolveNextIndex(fromAutoCompletion: Boolean = false): Int {
        if (activeQueue.isEmpty()) return -1
        if (fromAutoCompletion && repeatMode == RepeatModeState.ONE) {
            return currentIndex
        }
        return if (currentIndex + 1 < activeQueue.size) {
            currentIndex + 1
        } else if (repeatMode == RepeatModeState.ALL) {
            0
        } else {
            -1
        }
    }

    fun resolvePreviousIndex(currentPositionMs: Long): Int {
        if (activeQueue.isEmpty()) return -1
        if (currentPositionMs > 3500L) {
            return currentIndex
        }
        return if (currentIndex - 1 >= 0) {
            currentIndex - 1
        } else if (repeatMode == RepeatModeState.ALL) {
            activeQueue.lastIndex
        } else {
            0
        }
    }

    private fun <T> MutableList<T>.removeFirstOrNull(predicate: (T) -> Boolean): T? {
        val idx = indexOfFirst(predicate)
        return if (idx >= 0) removeAt(idx) else null
    }
}
