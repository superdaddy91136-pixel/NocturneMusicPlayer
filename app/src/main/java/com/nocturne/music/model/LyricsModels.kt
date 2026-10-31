package com.nocturne.music.model

import androidx.compose.runtime.Immutable

enum class LyricsSource(val label: String) {
    NONE("No Lyrics"),
    LOCAL_LRC("Local .LRC File"),
    IMPORTED_LRC("Imported .LRC"),
    EMBEDDED("Embedded Audio Tag"),
    ONLINE_CACHE("Cached Online Lyrics"),
    UNSYNCED_TEXT("Plain Lyrics")
}

@Immutable
data class LyricsWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

@Immutable
data class LyricsLine(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LyricsWord> = emptyList()
) {
    val hasWordTimestamps: Boolean
        get() = words.isNotEmpty()
}

@Immutable
data class LyricsDocument(
    val songId: Long,
    val isSynced: Boolean,
    val hasWordSync: Boolean,
    val source: LyricsSource,
    val offsetMs: Long = 0L,
    val lines: List<LyricsLine> = emptyList(),
    val rawText: String = "",
    val lrcFilePath: String? = null
) {
    fun findActiveLineIndex(positionMs: Long): Int {
        if (!isSynced || lines.isEmpty()) return -1
        val adjustedPos = (positionMs - offsetMs).coerceAtLeast(0L)
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].startTimeMs <= adjustedPos) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
