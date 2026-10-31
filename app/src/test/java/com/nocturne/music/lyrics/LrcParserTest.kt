package com.nocturne.music.lyrics

import com.nocturne.music.model.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse standard LRC timestamps and metadata offset`() {
        val raw = """
            [ti:Midnight Horizon]
            [ar:Nocturne Ensemble]
            [offset:250]
            [00:10.50]First line in the dark
            [00:15.20]Second line under glass
            [00:21.00]Third line fading out
        """.trimIndent()

        val doc = LrcParser.parse(
            songId = 42L,
            rawText = raw,
            source = LyricsSource.LOCAL_LRC
        )

        assertTrue(doc.isSynced)
        assertFalse(doc.hasWordSync)
        assertEquals(250L, doc.offsetMs)
        assertEquals(3, doc.lines.size)
        assertEquals(10_500L, doc.lines[0].startTimeMs)
        assertEquals("First line in the dark", doc.lines[0].text)
        assertEquals(15_200L, doc.lines[1].startTimeMs)
        assertEquals(21_000L, doc.lines[2].startTimeMs)

        // Binary search active line index accounting for +250ms offset
        assertEquals(-1, doc.findActiveLineIndex(10_000L))
        assertEquals(0, doc.findActiveLineIndex(10_800L))
        assertEquals(1, doc.findActiveLineIndex(16_000L))
        assertEquals(2, doc.findActiveLineIndex(25_000L))
    }

    @Test
    fun `parse enhanced word-level karaoke LRC timestamps`() {
        val raw = """
            [00:05.00]<00:05.00>Liquid <00:05.60>glass <00:06.20>horizon
        """.trimIndent()

        val doc = LrcParser.parse(
            songId = 99L,
            rawText = raw,
            source = LyricsSource.LOCAL_LRC
        )

        assertTrue(doc.isSynced)
        assertTrue(doc.hasWordSync)
        assertEquals(1, doc.lines.size)
        val line = doc.lines.first()
        assertEquals(3, line.words.size)
        assertEquals("Liquid ", line.words[0].text)
        assertEquals(5_000L, line.words[0].startTimeMs)
        assertEquals(5_600L, line.words[0].endTimeMs)
    }

    @Test
    fun `merge bilingual translation lines sharing identical timestamp`() {
        val raw = """
            [00:12.00]Hikari no naka e
            [00:12.00]Into the light
        """.trimIndent()

        val doc = LrcParser.parse(1L, raw, LyricsSource.EMBEDDED)
        assertTrue(doc.isSynced)
        assertEquals(1, doc.lines.size)
        assertEquals("Hikari no naka e", doc.lines[0].text)
        assertEquals("Into the light", doc.lines[0].translation)
    }

    @Test
    fun `fallback cleanly to unsynchronized plain text when no timestamps exist`() {
        val raw = """
            Static verse line one
            Static verse line two
        """.trimIndent()

        val doc = LrcParser.parse(7L, raw, LyricsSource.EMBEDDED)
        assertFalse(doc.isSynced)
        assertEquals(2, doc.lines.size)
        assertNotNull(doc.lines[0].text)
    }
}
