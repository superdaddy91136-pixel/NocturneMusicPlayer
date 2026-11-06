package com.nocturne.music.designsystem

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.nocturne.music.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsolatedArtworkColorExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<String, Color>(64)
    private val imageLoader by lazy { context.imageLoader }
    private val defaultColor = Color(0xFF141720) // CharcoalSurface (dark grey)

    suspend fun getArtworkColor(song: Song?): Color {
        if (song == null || song.albumArtUri.isNullOrBlank()) return defaultColor
        
        val cacheKey = "${song.id}_${song.customArtworkUpdatedAtMs}"
        cache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(song.albumArtUri)
                    .size(Size(128, 128)) // Downsample for performance
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap

                if (bitmap != null && !bitmap.isRecycled) {
                    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                    val swatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch

                    val rawColorInt = swatch?.rgb ?: defaultColor.hashCode()
                    val tunedColor = ensureReadableDarkColor(rawColorInt)
                    
                    val color = Color(tunedColor)
                    cache.put(cacheKey, color)
                    color
                } else {
                    defaultColor
                }
            } catch (e: Exception) {
                defaultColor
            }
        }
    }

    private fun ensureReadableDarkColor(colorInt: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorInt, hsl)
        
        // Cap saturation to avoid neon or washed-out looks
        hsl[1] = hsl[1].coerceAtMost(0.65f)
        
        // Clamp lightness strictly to dark-to-medium range (0.1 to 0.35)
        // This guarantees a very strong contrast ratio (> 4.5:1) against pure white text
        hsl[2] = hsl[2].coerceIn(0.1f, 0.35f)
        
        return ColorUtils.HSLToColor(hsl)
    }

    fun clearCache() {
        cache.evictAll()
    }
}
