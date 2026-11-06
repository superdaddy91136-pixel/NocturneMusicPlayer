package com.nocturne.music.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Nocturne's permanent dark-only palette combining pure AMOLED matte black,
 * deep obsidian charcoal, and selective liquid-glass specular highlights.
 */
object NocturnePalette {
    // Core Matte Backgrounds (AMOLED-first)
    val AmoledBlack = Color(0xFF050507)
    val ObsidianDeep = Color(0xFF090A0E)
    val ObsidianSurface = Color(0xFF0E1017)
    val CharcoalSurface = Color(0xFF141720)
    val CharcoalElevated = Color(0xFF1B1F2B)
    val CharcoalHover = Color(0xFF232837)

    // Selective Liquid-Glass Fills & Specular Borders
    val GlassSurfaceSubtle = Color(0x12FFFFFF)
    val GlassSurfaceMedium = Color(0x1CFFFFFF)
    val GlassSurfaceElevated = Color(0x28FFFFFF)
    val GlassBorderSpecular = Color(0x36FFFFFF)
    val GlassBorderHairline = Color(0x16FFFFFF)
    val GlassShadowTint = Color(0x99000000)

    // Primary & Accent Hues
    val LiquidIndigo = Color(0xFF88A4FF)
    val LiquidViolet = Color(0xFFA98BFF)
    val LiquidCyan = Color(0xFF6AE2FF)
    val WarmAmber = Color(0xFFFFB86C)
    val EmeraldGlow = Color(0xFF57CF8F)
    val CrimsonRose = Color(0xFFF05B78)

    // Typography Hierarchy
    val TextPrimary = Color(0xFFF5F7FB)
    val TextSecondary = Color(0xFFA5AEBF)
    val TextMuted = Color(0xFF667085)
    val TextDisabled = Color(0xFF424959)
}

@Immutable
data class ArtworkAtmosphere(
    val primaryAccent: Color = Color.White,
    val secondaryGlow: Color = Color.White.copy(alpha = 0.7f),
    val deepBackdrop: Color = NocturnePalette.ObsidianDeep,
    val highlightPill: Color = Color(0x33FFFFFF)
)
