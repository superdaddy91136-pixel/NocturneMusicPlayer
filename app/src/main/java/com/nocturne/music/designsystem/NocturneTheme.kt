package com.nocturne.music.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NocturneDarkColorScheme = darkColorScheme(
    primary = NocturnePalette.LiquidIndigo,
    onPrimary = NocturnePalette.AmoledBlack,
    primaryContainer = NocturnePalette.CharcoalElevated,
    onPrimaryContainer = NocturnePalette.TextPrimary,
    secondary = NocturnePalette.LiquidCyan,
    onSecondary = NocturnePalette.AmoledBlack,
    secondaryContainer = NocturnePalette.CharcoalSurface,
    onSecondaryContainer = NocturnePalette.TextSecondary,
    tertiary = NocturnePalette.WarmAmber,
    onTertiary = NocturnePalette.AmoledBlack,
    background = NocturnePalette.AmoledBlack,
    onBackground = NocturnePalette.TextPrimary,
    surface = NocturnePalette.ObsidianSurface,
    onSurface = NocturnePalette.TextPrimary,
    surfaceVariant = NocturnePalette.CharcoalSurface,
    onSurfaceVariant = NocturnePalette.TextSecondary,
    outline = NocturnePalette.GlassBorderHairline,
    outlineVariant = NocturnePalette.GlassSurfaceSubtle,
    error = NocturnePalette.CrimsonRose,
    onError = NocturnePalette.AmoledBlack
)

val LocalArtworkAtmosphere = staticCompositionLocalOf { ArtworkAtmosphere() }

/**
 * Strictly dark-only theme for Nocturne. No light mode or theme switcher exists by design.
 */
@Composable
fun NocturneTheme(
    atmosphere: ArtworkAtmosphere = ArtworkAtmosphere(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    val dynamicScheme = NocturneDarkColorScheme.copy(
        primary = atmosphere.primaryAccent,
        secondary = atmosphere.secondaryGlow
    )

    CompositionLocalProvider(LocalArtworkAtmosphere provides atmosphere) {
        MaterialTheme(
            colorScheme = dynamicScheme,
            typography = NocturneTypography,
            content = content
        )
    }
}
