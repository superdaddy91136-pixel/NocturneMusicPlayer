package com.nocturne.music.library

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun saveCroppedArtworkToAppStorage(songId: Long, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val artworkDir = File(context.filesDir, "artwork")
        if (!artworkDir.exists()) artworkDir.mkdirs()

        val maxDim = 1000
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap

        val outFile = File(artworkDir, "${songId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        outFile.absolutePath
    }

    suspend fun writeArtworkToAudioFile(filePath: String, bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val maxDim = 1000
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val imageData = stream.toByteArray()

            val originalFile = File(filePath)
            val ext = originalFile.extension
            val tempFile = File(context.cacheDir, "temp_artwork_update.$ext")
            
            // 1. Copy to temp
            originalFile.copyTo(tempFile, overwrite = true)
            
            // 2. Tag temp
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            val artwork = ArtworkFactory.getNew()
            artwork.binaryData = imageData
            artwork.mimeType = "image/jpeg"
            tag.deleteArtworkField()
            tag.setField(artwork)
            audioFile.commit()
            
            // 3. Copy back to original. 
            // In Android 11+, if we have the MediaStore write request granted, this should work if it's the exact path.
            // If the user hasn't granted it, this will throw an exception, which runCatching will catch.
            tempFile.copyTo(originalFile, overwrite = true)
            tempFile.delete()
            
            // Trigger rescan
            MediaScannerConnection.scanFile(context, arrayOf(originalFile.absolutePath), null, null)
            Unit
        }
    }
}
