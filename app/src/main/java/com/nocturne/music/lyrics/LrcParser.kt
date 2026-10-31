package com.nocturne.music.lyrics

import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.LyricsLine
import com.nocturne.music.model.LyricsSource
import com.nocturne.music.model.LyricsWord

/**
 * Full-featured LRC and Enhanced LRC (word-level karaoke) parser.
 * Supports:
 * - Standard timestamps: [mm:ss.xx], [mm:ss.xxx], [mm:ss:xx], [mm:ss]
 * - Multiple timestamps per line: [00:12.00][00:42.50]Repeated lyric line
 * - Metadata offset tag: [offset:+/-ms]
 * - Enhanced LRC word-level tags: <mm:ss.xx>Word or inline [mm:ss.xx]Word
 * - Bilingual/translation pairing when two lines share the exact same timestamp
 * - Clean fallback to unsynchronized plain lines when no timestamps exist
 */
object LrcParser {

    private val LINE_TIMESTAMP_REGEX = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val WORD_TIMESTAMP_REGEX = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>([^<]*)")
    private val OFFSET_TAG_REGEX = Regex("^\\[offset:\\s*([+-]?\\d+)\\s*\\]$", RegexOption.IGNORE_CASE)
    private val METADATA_TAG_REGEX = Regex("^\\[(ar|ti|al|by|re|ve|length|au|id):.*\\]$", RegexOption.IGNORE_CASE)

    fun parse(
        songId: Long,
        rawText: String,
        source: LyricsSource,
        customOffsetMs: Long = 0L,
        lrcFilePath: String? = null
    ): LyricsDocument {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) {
            return LyricsDocument(
                songId = songId,
                isSynced = false,
                hasWordSync = false,
                source = LyricsSource.NONE,
                rawText = ""
            )
        }

        var embeddedOffsetMs = 0L
        val rawEntries = mutableListOf<ParsedRawLine>()

        for (rawLine in normalized.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val offsetMatch = OFFSET_TAG_REGEX.matchEntire(trimmed)
            if (offsetMatch != null) {
                embeddedOffsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }

            if (METADATA_TAG_REGEX.matches(trimmed)) {
                continue
            }

            val lineMatches = LINE_TIMESTAMP_REGEX.findAll(trimmed).toList()
            if (lineMatches.isEmpty()) continue

            // Extract leading timestamps [mm:ss.xx][mm:ss.xx]
            val leadingTimestamps = mutableListOf<Long>()
            var lastLeadingEnd = 0
            for (match in lineMatches) {
                if (match.range.first == lastLeadingEnd) {
                    leadingTimestamps.add(parseTimestampMatch(match))
                    lastLeadingEnd = match.range.last + 1
                } else {
                    break
                }
            }

            if (leadingTimestamps.isEmpty()) continue

            val contentPart = trimmed.substring(lastLeadingEnd).trim()
            val wordMatches = WORD_TIMESTAMP_REGEX.findAll(contentPart).toList()

            val words = mutableListOf<LyricsWord>()
            val cleanText = if (wordMatches.isNotEmpty()) {
                val sb = StringBuilder()
                for (i in wordMatches.indices) {
                    val wm = wordMatches[i]
                    val startMs = parseTimestampParts(wm.groupValues[1], wm.groupValues[2], wm.groupValues[3])
                    val nextStartMs = if (i + 1 < wordMatches.size) {
                        val nextWm = wordMatches[i + 1]
                        parseTimestampParts(nextWm.groupValues[1], nextWm.groupValues[2], nextWm.groupValues[3])
                    } else {
                        startMs + 450L
                    }
                    val tokenText = wm.groupValues[4]
                    if (tokenText.isNotEmpty()) {
                        sb.append(tokenText)
                        words.add(
                            LyricsWord(
                                text = tokenText,
                                startTimeMs = startMs,
                                endTimeMs = nextStartMs.coerceAtLeast(startMs + 60L)
                            )
                        )
                    }
                }
                sb.toString().trim()
            } else {
                // Also check if inline [mm:ss.xx] word tags were used inside the line
                val remainingTimestamps = LINE_TIMESTAMP_REGEX.findAll(contentPart).toList()
                if (remainingTimestamps.isNotEmpty() && leadingTimestamps.size == 1) {
                    val firstWordStart = leadingTimestamps.first()
                    val firstSegment = contentPart.substring(0, remainingTimestamps.first().range.first)
                    val sb = StringBuilder()
                    if (firstSegment.isNotEmpty()) {
                        val firstEnd = parseTimestampMatch(remainingTimestamps.first())
                        sb.append(firstSegment)
                        words.add(LyricsWord(firstSegment, firstWordStart, firstEnd.coerceAtLeast(firstWordStart + 60L)))
                    }
                    for (i in remainingTimestamps.indices) {
                        val tm = remainingTimestamps[i]
                        val wStart = parseTimestampMatch(tm)
                        val segStart = tm.range.last + 1
                        val segEnd = if (i + 1 < remainingTimestamps.size) {
                            remainingTimestamps[i + 1].range.first
                        } else {
                            contentPart.length
                        }
                        val wEnd = if (i + 1 < remainingTimestamps.size) {
                            parseTimestampMatch(remainingTimestamps[i + 1])
                        } else {
                            wStart + 450L
                        }
                        val token = contentPart.substring(segStart, segEnd)
                        if (token.isNotEmpty()) {
                            sb.append(token)
                            words.add(LyricsWord(token, wStart, wEnd.coerceAtLeast(wStart + 60L)))
                        }
                    }
                    sb.toString().trim()
                } else {
                    contentPart
                }
            }

            for (ts in leadingTimestamps) {
                rawEntries.add(
                    ParsedRawLine(
                        startTimeMs = ts,
                        text = cleanText,
                        words = if (leadingTimestamps.size == 1) words else emptyList()
                    )
                )
            }
        }

        // If no synchronized timestamps were found, treat as plain unsynchronized lyrics
        if (rawEntries.isEmpty()) {
            val plainLines = normalized.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !METADATA_TAG_REGEX.matches(it) }
                .mapIndexed { idx, lineText ->
                    LyricsLine(
                        index = idx,
                        startTimeMs = idx * 4000L,
                        endTimeMs = (idx + 1) * 4000L,
                        text = lineText
                    )
                }
            return LyricsDocument(
                songId = songId,
                isSynced = false,
                hasWordSync = false,
                source = if (source == LyricsSource.NONE) LyricsSource.UNSYNCED_TEXT else source,
                offsetMs = customOffsetMs,
                lines = plainLines,
                rawText = normalized,
                lrcFilePath = lrcFilePath
            )
        }

        // Sort chronologically and merge same-timestamp translation lines
        val sorted = rawEntries.sortedBy { it.startTimeMs }
        val merged = mutableListOf<ParsedRawLine>()
        for (item in sorted) {
            val last = merged.lastOrNull()
            if (last != null && last.startTimeMs == item.startTimeMs && last.text.isNotBlank() && item.text.isNotBlank()) {
                merged[merged.lastIndex] = last.copy(
                    translation = if (last.translation == null) item.text else "${last.translation}\n${item.text}"
                )
            } else {
                merged.add(item)
            }
        }

        var anyWordSync = false
        val finalLines = merged.mapIndexed { index, item ->
            val nextStart = if (index + 1 < merged.size) {
                merged[index + 1].startTimeMs
            } else {
                item.startTimeMs + 6000L
            }
            val endTime = nextStart.coerceAtLeast(item.startTimeMs + 300L)
            val tunedWords = if (item.words.isNotEmpty()) {
                anyWordSync = true
                item.words.mapIndexed { wIdx, w ->
                    if (wIdx == item.words.lastIndex) {
                        w.copy(endTimeMs = endTime.coerceAtMost(w.startTimeMs + 2500L).coerceAtLeast(w.startTimeMs + 100L))
                    } else {
                        w
                    }
                }
            } else {
                emptyList()
            }
            LyricsLine(
                index = index,
                startTimeMs = item.startTimeMs,
                endTimeMs = endTime,
                text = item.text.ifBlank { "♪" },
                translation = item.translation,
                words = tunedWords
            )
        }

        return LyricsDocument(
            songId = songId,
            isSynced = true,
            hasWordSync = anyWordSync,
            source = source,
            offsetMs = customOffsetMs + embeddedOffsetMs,
            lines = finalLines,
            rawText = normalized,
            lrcFilePath = lrcFilePath
        )
    }

    private fun parseTimestampMatch(match: MatchResult): Long {
        return parseTimestampParts(
            match.groupValues[1],
            match.groupValues[2],
            match.groupValues.getOrElse(3) { "" }
        )
    }

    private fun parseTimestampParts(minStr: String, secStr: String, fracStr: String): Long {
        val minutes = minStr.toLongOrNull() ?: 0L
        val seconds = secStr.toLongOrNull() ?: 0L
        val millis = when (fracStr.length) {
            0 -> 0L
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (minutes * 60_000L) + (seconds * 1_000L) + millis
    }

    private data class ParsedRawLine(
        val startTimeMs: Long,
        val text: String,
        val translation: String? = null,
        val words: List<LyricsWord> = emptyList()
    )
}
