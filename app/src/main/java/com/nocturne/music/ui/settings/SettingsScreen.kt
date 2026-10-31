package com.nocturne.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.DynamicBackgroundStyle
import com.nocturne.music.model.EqualizerState
import com.nocturne.music.model.LyricsFontSize
import com.nocturne.music.model.UserSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    equalizerState: EqualizerState,
    excludedFolders: List<String>,
    cachedLyricsCount: Int,
    totalSongsCount: Int,
    onBack: () -> Unit,
    onOpenEqualizerSheet: () -> Unit,
    onSetGaplessPlayback: (Boolean) -> Unit,
    onSetCrossfadeSec: (Int) -> Unit,
    onSetVolumeNormalization: (Boolean) -> Unit,
    onSetPreampDb: (Float) -> Unit,
    onSetRememberPosition: (Boolean) -> Unit,
    onSetResumeOnBluetooth: (Boolean) -> Unit,
    onSetPauseOnZeroVolume: (Boolean) -> Unit,
    onSetWordLevelKaraoke: (Boolean) -> Unit,
    onSetPreferEmbeddedLyrics: (Boolean) -> Unit,
    onSetAutoFetchOnlineLyrics: (Boolean) -> Unit,
    onSetKeepScreenOnLyrics: (Boolean) -> Unit,
    onSetLyricsFontSize: (LyricsFontSize) -> Unit,
    onSetDynamicBackgroundStyle: (DynamicBackgroundStyle) -> Unit,
    onSetHighQualityArtwork: (Boolean) -> Unit,
    onSetSmoothArtworkTransitions: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetMinTrackDurationSec: (Int) -> Unit,
    onSetIgnoreRingtones: (Boolean) -> Unit,
    onRestoreExcludedFolder: (String) -> Unit,
    onRescanLibrary: () -> Unit,
    onClearLyricsCache: () -> Unit,
    onClearArtworkCache: () -> Unit,
    onClearPlayHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NocturnePalette.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NocturnePalette.TextPrimary
                    )
                    Text(
                        text = "Permanent AMOLED Dark Architecture • Offline Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturnePalette.TextMuted
                    )
                }
            }
        }

        // 1. Playback Behaviour & Crossfade
        item {
            SettingsGroupCard(title = "Playback & Crossfade", icon = Icons.Rounded.PlayCircle) {
                SettingsToggleRow(
                    title = "Gapless Playback",
                    subtitle = "Eliminate silence between consecutive album tracks",
                    checked = userSettings.gaplessPlayback,
                    onCheckedChange = onSetGaplessPlayback
                )
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Crossfade Duration", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (userSettings.crossfadeDurationSec == 0) "Off" else "${userSettings.crossfadeDurationSec} sec",
                            style = MaterialTheme.typography.labelLarge,
                            color = atmosphere.primaryAccent
                        )
                    }
                    Slider(
                        value = userSettings.crossfadeDurationSec.toFloat(),
                        onValueChange = { onSetCrossfadeSec(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = atmosphere.primaryAccent,
                            activeTrackColor = atmosphere.primaryAccent
                        )
                    )
                }
                SettingsToggleRow(
                    title = "Restore Playback Position on Launch",
                    subtitle = "Remember active queue and exact timestamp across restarts",
                    checked = userSettings.rememberPlaybackPosition,
                    onCheckedChange = onSetRememberPosition
                )
                SettingsToggleRow(
                    title = "Resume on Bluetooth / Headset Connect",
                    subtitle = "Automatically resume when trusted audio output connects",
                    checked = userSettings.resumeOnBluetoothConnect,
                    onCheckedChange = onSetResumeOnBluetooth
                )
                SettingsToggleRow(
                    title = "Pause When Volume Reaches Zero",
                    subtitle = "Prevent silent background playback",
                    checked = userSettings.pauseOnZeroVolume,
                    onCheckedChange = onSetPauseOnZeroVolume
                )
            }
        }

        // 2. Equalizer & ReplayGain Volume Normalization
        item {
            SettingsGroupCard(title = "Equalizer & Normalization", icon = Icons.Rounded.Equalizer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NocturnePalette.CharcoalElevated)
                        .clickable(onClick = onOpenEqualizerSheet)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Open 5-Band Reference Equalizer & DSP",
                            style = MaterialTheme.typography.titleMedium,
                            color = NocturnePalette.TextPrimary
                        )
                        Text(
                            text = if (equalizerState.enabled) {
                                "Active Preset: ${equalizerState.currentPresetName}"
                            } else {
                                "Bypassed (Bit-perfect flat output)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = atmosphere.primaryAccent
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = atmosphere.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "ReplayGain & Volume Normalization",
                    subtitle = "Balance perceived loudness across tracks",
                    checked = userSettings.volumeNormalization,
                    onCheckedChange = onSetVolumeNormalization
                )

                if (userSettings.volumeNormalization) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Normalization Preamp Gain", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = String.format("%+.1f dB", userSettings.normalizationPreampDb),
                                style = MaterialTheme.typography.labelSmall,
                                color = atmosphere.primaryAccent
                            )
                        }
                        Slider(
                            value = userSettings.normalizationPreampDb,
                            onValueChange = { onSetPreampDb((it * 2f).roundToInt() / 2f) },
                            valueRange = -6f..6f,
                            colors = SliderDefaults.colors(
                                thumbColor = atmosphere.primaryAccent,
                                activeTrackColor = atmosphere.primaryAccent
                            )
                        )
                    }
                }
            }
        }

        // 3. Synchronized Lyrics Preferences
        item {
            SettingsGroupCard(title = "Synchronized Lyrics", icon = Icons.Rounded.Lyrics) {
                SettingsToggleRow(
                    title = "Word-Level Karaoke Highlighting",
                    subtitle = "Progressive word illumination when Enhanced LRC timestamps exist",
                    checked = userSettings.wordLevelKaraokeHighlight,
                    onCheckedChange = onSetWordLevelKaraoke
                )
                SettingsToggleRow(
                    title = "Prefer Embedded Audio Tag Lyrics",
                    subtitle = "Check ID3v2/Vorbis/MP4 embedded lyrics before sidecar .LRC files",
                    checked = userSettings.preferEmbeddedLyrics,
                    onCheckedChange = onSetPreferEmbeddedLyrics
                )
                SettingsToggleRow(
                    title = "Auto-Fetch & Cache Online Lyrics",
                    subtitle = "Query LRCLIB when local .LRC or embedded tags are absent",
                    checked = userSettings.autoFetchOnlineLyrics,
                    onCheckedChange = onSetAutoFetchOnlineLyrics
                )
                SettingsToggleRow(
                    title = "Keep Screen On During Lyrics",
                    subtitle = "Prevent screen dimming while viewing synchronized lyrics",
                    checked = userSettings.keepScreenOnDuringLyrics,
                    onCheckedChange = onSetKeepScreenOnLyrics
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Lyrics Typography Scale", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LyricsFontSize.entries.forEach { sizeOption ->
                        val selected = userSettings.lyricsFontSize == sizeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) atmosphere.primaryAccent else NocturnePalette.CharcoalElevated
                                )
                                .clickable { onSetLyricsFontSize(sizeOption) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sizeOption.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) NocturnePalette.AmoledBlack else NocturnePalette.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Artwork, Dynamic Backgrounds & Animations
        item {
            SettingsGroupCard(title = "Artwork, Surface & Motion", icon = Icons.Rounded.Album) {
                Text("Now Playing Atmosphere Mode", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                DynamicBackgroundStyle.entries.forEach { styleOption ->
                    val selected = userSettings.dynamicBackgroundStyle == styleOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) atmosphere.primaryAccent.copy(alpha = 0.16f) else NocturnePalette.CharcoalElevated
                            )
                            .clickable { onSetDynamicBackgroundStyle(styleOption) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = styleOption.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) atmosphere.primaryAccent else NocturnePalette.TextPrimary
                            )
                            Text(
                                text = styleOption.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturnePalette.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                SettingsToggleRow(
                    title = "High-Resolution Artwork Caching",
                    subtitle = "Cache full-resolution album covers in 128 MB disk cache",
                    checked = userSettings.highQualityArtworkCache,
                    onCheckedChange = onSetHighQualityArtwork
                )
                SettingsToggleRow(
                    title = "Smooth Artwork Crossfade Transitions",
                    subtitle = "Animate cover art changes when switching tracks",
                    checked = userSettings.smoothArtworkTransitions,
                    onCheckedChange = onSetSmoothArtworkTransitions
                )
                SettingsToggleRow(
                    title = "Reduced Motion",
                    subtitle = "Minimize spring and scale animations across the interface",
                    checked = userSettings.reducedMotion,
                    onCheckedChange = onSetReducedMotion
                )
            }
        }

        // 5. Library Scanning & Excluded Folders
        item {
            SettingsGroupCard(title = "Library Scanning & Folders", icon = Icons.Rounded.Folder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Indexed Local Tracks: $totalSongsCount", style = MaterialTheme.typography.titleMedium)
                        Text("Direct MediaStore + audio container parser", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(atmosphere.primaryAccent)
                            .clickable(onClick = onRescanLibrary)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Rescan Now", style = MaterialTheme.typography.labelLarge, color = NocturnePalette.AmoledBlack)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Minimum Track Duration Filter", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${userSettings.minimumTrackDurationSec} sec",
                        style = MaterialTheme.typography.labelSmall,
                        color = atmosphere.primaryAccent
                    )
                }
                Slider(
                    value = userSettings.minimumTrackDurationSec.toFloat(),
                    onValueChange = { onSetMinTrackDurationSec(it.roundToInt()) },
                    valueRange = 0f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = atmosphere.primaryAccent,
                        activeTrackColor = atmosphere.primaryAccent
                    )
                )

                SettingsToggleRow(
                    title = "Ignore Ringtones, Alarms & Voice Notes",
                    subtitle = "Skip notification sounds and short voice clips during scan",
                    checked = userSettings.ignoreRingtoneAndNotificationFolders,
                    onCheckedChange = onSetIgnoreRingtones
                )

                if (excludedFolders.isNotEmpty()) {
                    HorizontalDivider(
                        color = NocturnePalette.GlassBorderHairline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Excluded Folders (${excludedFolders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = NocturnePalette.TextPrimary
                    )
                    excludedFolders.forEach { path ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = path,
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturnePalette.TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Restore",
                                style = MaterialTheme.typography.labelMedium,
                                color = atmosphere.primaryAccent,
                                modifier = Modifier
                                    .clickable { onRestoreExcludedFolder(path) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Storage & Cache Management
        item {
            SettingsGroupCard(title = "Storage & Cache Management", icon = Icons.Rounded.Storage) {
                SettingsActionButtonRow(
                    title = "Offline Lyrics Cache ($cachedLyricsCount tracks cached)",
                    actionLabel = "Clear Lyrics Cache",
                    onClick = onClearLyricsCache
                )
                SettingsActionButtonRow(
                    title = "Artwork Memory & Disk Cache",
                    actionLabel = "Purge Artwork Cache",
                    onClick = onClearArtworkCache
                )
                SettingsActionButtonRow(
                    title = "Listening History Log",
                    actionLabel = "Clear History",
                    onClick = onClearPlayHistory
                )
            }
        }

        // 7. Application Information
        item {
            SettingsGroupCard(title = "About Nocturne Audio", icon = Icons.Rounded.Info) {
                Text(
                    text = "Nocturne Reference Offline Music Player v1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = NocturnePalette.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Engineered with Kotlin, Jetpack Compose, AndroidX Media3 ExoPlayer, Room, DataStore, and Hilt. Strictly dark-only AMOLED matte black & selective liquid-glass interface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "100% Offline-First • Zero Ads • Zero Analytics • Zero Telemetry",
                    style = MaterialTheme.typography.labelSmall,
                    color = atmosphere.primaryAccent
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .matteSurface(RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = atmosphere.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NocturnePalette.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NocturnePalette.TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = NocturnePalette.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = atmosphere.primaryAccent)
        )
    }
}

@Composable
private fun SettingsActionButtonRow(
    title: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = NocturnePalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NocturnePalette.CharcoalElevated)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = NocturnePalette.TextPrimary
            )
        }
    }
}
