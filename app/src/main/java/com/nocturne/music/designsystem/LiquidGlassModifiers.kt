package com.nocturne.music.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nocturne.music.model.DynamicBackgroundStyle

/**
 * Selective liquid-glass panel modifier: combines a translucent frosted fill,
 * subtle inner gradient sheen, and a directional specular border without
 * incurring expensive full-screen real-time blur passes during list scrolling.
 */
fun Modifier.liquidGlassPanel(
    shape: Shape,
    tintColor: Color = Color.White,
    fillAlpha: Float = 0.11f,
    borderAlpha: Float = 0.22f,
    borderWidth: Dp = 0.8.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = fillAlpha),
                tintColor.copy(alpha = (fillAlpha * 0.65f).coerceAtMost(0.18f)),
                NocturnePalette.ObsidianSurface.copy(alpha = 0.88f)
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 900f)
        ),
        shape = shape
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                tintColor.copy(alpha = borderAlpha * 0.6f),
                Color.White.copy(alpha = borderAlpha * 0.15f)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 600f)
        ),
        shape = shape
    )

/**
 * Refined matte charcoal surface for lists and secondary containers.
 */
fun Modifier.matteSurface(
    shape: Shape,
    surfaceColor: Color = NocturnePalette.ObsidianSurface,
    borderColor: Color = NocturnePalette.GlassBorderHairline
): Modifier = this
    .clip(shape)
    .background(color = surfaceColor, shape = shape)
    .border(width = 0.6.dp, color = borderColor, shape = shape)

/**
 * Full-screen dynamic ambient background derived from the current track's album artwork.
 */
@Composable
fun Modifier.artworkAmbientCanvas(
    atmosphere: ArtworkAtmosphere,
    style: DynamicBackgroundStyle = DynamicBackgroundStyle.LIQUID_AMBIENT,
    intensity: Float = 1.0f
): Modifier = this.background(NocturnePalette.AmoledBlack)
