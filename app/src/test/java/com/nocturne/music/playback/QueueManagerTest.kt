package com.nocturne.music.playback

import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueManagerTest {

    private fun sampleSong(id: Long, title: String = "Song $id") = Song(
        id = id,
        title = title,
        artist = "Artist",
        artistId = 1L,
        album = "Album",
        albumId = 1L,
        durationMs = 180_000L,
        filePath = "/storage/emulated/0/Music/$id.mp3",
        folderPath = "/storage/emulated/0/Music",
        folderName = "Music",
        contentUri = "content://media/external/audio/media/$id",
        albumArtUri = null
    )

    @Test
    fun `setQueue and shuffle preserve current playing track and restore order`() {
        val manager = QueueManager(Random(42))
        val songs = (1L..6L).map { sampleSong(it) }

        manager.setQueue(songs, startIndex = 2, shuffle = false)
        assertEquals(3L, manager.currentSong?.id)
        assertEquals(2, manager.currentIndex)

        // Enable shuffle: current song (3L) stays active at index 0
        val shuffledIdx = manager.setShuffle(true)
        assertEquals(0, shuffledIdx)
        assertEquals(3L, manager.currentSong?.id)
        assertEquals(6, manager.currentQueue.size)

        // Disable shuffle: original order restored and currentIndex points back to song 3L
        val restoredIdx = manager.setShuffle(false)
        assertEquals(2, restoredIdx)
        assertEquals(3L, manager.currentSong?.id)
        assertEquals(songs.map { it.id }, manager.currentQueue.map { it.id })
    }

    @Test
    fun `playNext and addToQueue insert at expected positions`() {
        val manager = QueueManager()
        val songs = listOf(sampleSong(1L), sampleSong(2L))
        manager.setQueue(songs, startIndex = 0, shuffle = false)

        val insertedIndex = manager.playNext(sampleSong(99L))
        assertEquals(1, insertedIndex)
        assertEquals(listOf(1L, 99L, 2L), manager.currentQueue.map { it.id })

        val tailIndex = manager.addToQueue(sampleSong(100L))
        assertEquals(3, tailIndex)
        assertEquals(listOf(1L, 99L, 2L, 100L), manager.currentQueue.map { it.id })
    }

    @Test
    fun `moveQueueItem and removeQueueItem keep currentIndex anchored`() {
        val manager = QueueManager()
        val songs = listOf(sampleSong(10L), sampleSong(20L), sampleSong(30L), sampleSong(40L))
        manager.setQueue(songs, startIndex = 2, shuffle = false) // playing 30L

        assertTrue(manager.moveQueueItem(0, 3))
        assertEquals(1, manager.currentIndex)
        assertEquals(30L, manager.currentSong?.id)

        assertTrue(manager.removeQueueItem(0))
        assertEquals(0, manager.currentIndex)
        assertEquals(30L, manager.currentSong?.id)
    }

    @Test
    fun `resolveNextIndex honors RepeatModeState OFF ALL and ONE`() {
        val manager = QueueManager()
        val songs = listOf(sampleSong(1L), sampleSong(2L))
        manager.setQueue(songs, startIndex = 1, shuffle = false)

        manager.setRepeatMode(RepeatModeState.OFF)
        assertEquals(-1, manager.resolveNextIndex(fromAutoCompletion = true))

        manager.setRepeatMode(RepeatModeState.ALL)
        assertEquals(0, manager.resolveNextIndex(fromAutoCompletion = true))

        manager.setRepeatMode(RepeatModeState.ONE)
        assertEquals(1, manager.resolveNextIndex(fromAutoCompletion = true))
    }
}
