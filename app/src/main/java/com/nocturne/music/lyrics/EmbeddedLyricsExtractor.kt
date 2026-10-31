package com.nocturne.music.lyrics

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts embedded lyrics directly from local audio containers:
 * - ID3v2 (MP3): USLT (unsynchronized/LRC text in frame) and TXXX (LYRICS)
 * - Vorbis Comments (FLAC / OGG / OPUS): LYRICS= or UNSYNCEDLYRICS=
 * - MP4 / M4A: ©lyr atom
 */
@Singleton
class EmbeddedLyricsExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun extractEmbeddedLyrics(filePath: String, contentUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = if (filePath.isNotBlank()) File(filePath) else null
            val bytes: ByteArray? = when {
                file != null && file.exists() && file.canRead() -> {
                    file.inputStream().use { readHeaderAndFooterWindow(it, file.length()) }
                }
                contentUri.isNotBlank() -> {
                    context.contentResolver.openInputStream(Uri.parse(contentUri))?.use { stream ->
                        readStreamWindow(stream, maxBytes = 512 * 1024)
                    }
                }
                else -> null
            }

            if (bytes == null || bytes.size < 16) return@withContext null

            // 1. Check ID3v2 header
            if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
                parseId3v2Lyrics(bytes)?.let { if (it.isNotBlank()) return@withContext it }
            }

            // 2. Check Vorbis comment keys ("LYRICS=" or "UNSYNCEDLYRICS=")
            parseVorbisCommentLyrics(bytes)?.let { if (it.isNotBlank()) return@withContext it }

            // 3. Check MP4/M4A ©lyr atom
            parseMp4LyricsAtom(bytes)?.let { if (it.isNotBlank()) return@withContext it }

            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun readStreamWindow(input: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes)
        var total = 0
        while (total < maxBytes) {
            val read = input.read(buffer, total, maxBytes - total)
            if (read <= 0) break
            total += read
        }
        return if (total == maxBytes) buffer else buffer.copyOf(total)
    }

    private fun readHeaderAndFooterWindow(input: InputStream, length: Long): ByteArray {
        val cap = minOf(length, 512 * 1024L).toInt()
        return readStreamWindow(input, cap)
    }

    private fun parseId3v2Lyrics(data: ByteArray): String? {
        if (data.size < 10) return null
        val version = data[3].toInt() and 0xFF
        val tagSize = synchsafeToInt(data, 6).coerceAtMost(data.size - 10)
        var pos = 10
        val limit = (10 + tagSize).coerceAtMost(data.size)

        while (pos + 10 < limit) {
            val frameId = String(data, pos, 4, Charsets.ISO_8859_1)
            if (!frameId.all { it in 'A'..'Z' || it in '0'..'9' }) break

            val frameSize = if (version == 4) {
                synchsafeToInt(data, pos + 4)
            } else {
                ((data[pos + 4].toInt() and 0xFF) shl 24) or
                    ((data[pos + 5].toInt() and 0xFF) shl 16) or
                    ((data[pos + 6].toInt() and 0xFF) shl 8) or
                    (data[pos + 7].toInt() and 0xFF)
            }
            if (frameSize <= 0 || pos + 10 + frameSize > limit) break

            val frameStart = pos + 10
            if (frameId == "USLT" && frameSize > 4) {
                val encoding = data[frameStart].toInt() and 0xFF
                val charset = id3Charset(encoding)
                var textStart = frameStart + 4
                val frameEnd = frameStart + frameSize
                while (textStart < frameEnd && data[textStart] != 0.toByte()) {
                    textStart++
                }
                while (textStart < frameEnd && data[textStart] == 0.toByte()) {
                    textStart++
                }
                if (textStart < frameEnd) {
                    val text = String(data, textStart, frameEnd - textStart, charset).trim('\u0000', ' ', '\n', '\r')
                    if (text.isNotBlank()) return text
                }
            }
            pos += 10 + frameSize
        }
        return null
    }

    private fun parseVorbisCommentLyrics(data: ByteArray): String? {
        val ascii = String(data, Charsets.ISO_8859_1)
        val keys = listOf("LYRICS=", "UNSYNCEDLYRICS=", "lyrics=", "unsyncedlyrics=")
        for (key in keys) {
            val idx = ascii.indexOf(key)
            if (idx >= 4) {
                val commentLen = (data[idx - 4].toInt() and 0xFF) or
                    ((data[idx - 3].toInt() and 0xFF) shl 8) or
                    ((data[idx - 2].toInt() and 0xFF) shl 16) or
                    ((data[idx - 1].toInt() and 0xFF) shl 24)
                val valueStart = idx + key.length
                val valueLen = (commentLen - key.length).coerceIn(1, (data.size - valueStart).coerceAtLeast(1))
                if (valueStart + valueLen <= data.size && valueLen < 128_000) {
                    val decoded = String(data, valueStart, valueLen, Charsets.UTF_8).trim('\u0000', ' ')
                    if (decoded.length >= 8) return decoded
                }
            }
        }
        return null
    }

    private fun parseMp4LyricsAtom(data: ByteArray): String? {
        for (i in 0 until data.size - 24) {
            if ((data[i].toInt() and 0xFF) == 0xA9 &&
                data[i + 1] == 'l'.code.toByte() &&
                data[i + 2] == 'y'.code.toByte() &&
                data[i + 3] == 'r'.code.toByte()
            ) {
                val dataAtomOffset = i + 8
                if (dataAtomOffset + 16 < data.size &&
                    data[dataAtomOffset] == 'd'.code.toByte() &&
                    data[dataAtomOffset + 1] == 'a'.code.toByte() &&
                    data[dataAtomOffset + 2] == 't'.code.toByte() &&
                    data[dataAtomOffset + 3] == 'a'.code.toByte()
                ) {
                    val atomSize = ((data[i + 4].toInt() and 0xFF) shl 24) or
                        ((data[i + 5].toInt() and 0xFF) shl 16) or
                        ((data[i + 6].toInt() and 0xFF) shl 8) or
                        (data[i + 7].toInt() and 0xFF)
                    val textStart = dataAtomOffset + 12
                    val textLen = (atomSize - 16).coerceIn(1, (data.size - textStart).coerceAtLeast(1))
                    if (textStart + textLen <= data.size && textLen < 128_000) {
                        return String(data, textStart, textLen, Charsets.UTF_8).trim('\u0000', ' ')
                    }
                }
            }
        }
        return null
    }

    private fun synchsafeToInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun id3Charset(encodingByte: Int): Charset = when (encodingByte) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> Charsets.UTF_8
    }
}
