package com.nocturne.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nocturne.music.designsystem.NocturnePalette
import kotlin.math.abs

/**
 * Displays album or artist artwork with a custom audiophile vinyl/obsidian fallback
 * when artwork is missing or unreadable.
 */
@Composable
fun ArtworkImage(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") seedTitle: String = "",
    artworkKey: String? = null,
    iconSize: Dp = 28.dp,
    showBorder: Boolean = true
) {
    val context = LocalContext.current
    val baseModifier = modifier
        .clip(shape)
        .then(
            if (showBorder) {
                Modifier.border(
                    width = 0.7.dp,
                    color = NocturnePalette.GlassBorderHairline,
                    shape = shape
                )
            } else {
                Modifier
            }
        )

    if (artworkUri.isNullOrBlank()) {
        VinylArtworkFallback(
            iconSize = iconSize,
            modifier = baseModifier
        )
    } else {
        val request = remember(artworkUri, artworkKey, context) {
            ImageRequest.Builder(context)
                .data(artworkUri)
                .apply {
                    if (artworkKey != null) {
                        memoryCacheKey(artworkKey)
                        diskCacheKey(artworkKey)
                    }
                }
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = baseModifier,
            loading = {
                VinylArtworkFallback(
                    iconSize = iconSize,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                VinylArtworkFallback(
                    iconSize = iconSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun VinylArtworkFallback(
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(NocturnePalette.ObsidianDeep),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize)
        )
    }
}
