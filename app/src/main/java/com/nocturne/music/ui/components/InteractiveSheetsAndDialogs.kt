package com.nocturne.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.model.EqualizerState
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.SleepTimerState
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils
import kotlin.math.roundToInt

@Composable
fun TrackContextBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onEditMetadata: () -> Unit,
    onEditArtwork: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.ObsidianSurface,
        contentColor = NocturnePalette.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkImage(
                    artworkUri = song.albumArtUri,
                    artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                    contentDescription = song.title,
                    shape = RoundedCornerShape(12.dp),
                    seedTitle = song.album,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = NocturnePalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NocturnePalette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.audioQualityBadge} • ${TimeFormatUtils.formatFileSize(song.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturnePalette.TextMuted
                    )
                }
            }

            HorizontalDivider(
                color = NocturnePalette.GlassBorderHairline,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SheetOptionRow(Icons.Rounded.PlayArrow, "Play Immediately") {
                onPlayNow()
                onDismiss()
            }
            SheetOptionRow(Icons.Rounded.QueuePlayNext, "Play Next in Queue") {
                onPlayNext()
                onDismiss()
            }
            SheetOptionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to End of Queue") {
                onAddToQueue()
                onDismiss()
            }
            SheetOptionRow(
                icon = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (song.isFavorite) "Remove from Favourites" else "Add to Favourites"
            ) {
                onToggleFavorite()
                onDismiss()
            }
            SheetOptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Playlist…") {
                onDismiss()
                onAddToPlaylist()
            }
            SheetOptionRow(Icons.Rounded.Album, "View Album (${song.album})") {
                onDismiss()
                onGoToAlbum()
            }
            SheetOptionRow(Icons.Rounded.Person, "View Artist (${song.artist})") {
                onDismiss()
                onGoToArtist()
            }
            SheetOptionRow(Icons.Rounded.Edit, "Edit Metadata & Audio Details") {
                onDismiss()
                onEditMetadata()
            }
            SheetOptionRow(Icons.Rounded.ImageSearch, "Edit Artwork") {
                onDismiss()
                onEditArtwork()
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NocturnePalette.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = NocturnePalette.TextPrimary
        )
    }
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    val atmosphere = LocalArtworkAtmosphere.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.CharcoalSurface,
        titleContentColor = NocturnePalette.TextPrimary,
        textContentColor = NocturnePalette.TextSecondary,
        title = {
            Text("Add \"${song.title}\" to Playlist", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Create new playlist") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = atmosphere.primaryAccent,
                        unfocusedBorderColor = NocturnePalette.GlassBorderHairline,
                        focusedTextColor = NocturnePalette.TextPrimary,
                        unfocusedTextColor = NocturnePalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (newPlaylistName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(atmosphere.primaryAccent)
                            .clickable {
                                onCreateNewPlaylist(newPlaylistName.trim())
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create & Add Track",
                            style = MaterialTheme.typography.labelLarge,
                            color = NocturnePalette.AmoledBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                if (playlists.isEmpty()) {
                    Text(
                        text = "No existing playlists yet. Enter a name above to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NocturnePalette.TextMuted
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(210.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectPlaylist(playlist)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NocturnePalette.TextPrimary
                                )
                                Text(
                                    text = "${playlist.songCount} songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocturnePalette.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NocturnePalette.TextSecondary)
            }
        }
    )
}

@Composable
fun CreateOrRenamePlaylistDialog(
    initialName: String = "",
    initialDescription: String = "",
    titleText: String = "New Playlist",
    confirmLabel: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    val atmosphere = LocalArtworkAtmosphere.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.CharcoalSurface,
        title = { Text(titleText, color = NocturnePalette.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = atmosphere.primaryAccent,
                        unfocusedBorderColor = NocturnePalette.GlassBorderHairline,
                        focusedTextColor = NocturnePalette.TextPrimary,
                        unfocusedTextColor = NocturnePalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = atmosphere.primaryAccent,
                        unfocusedBorderColor = NocturnePalette.GlassBorderHairline,
                        focusedTextColor = NocturnePalette.TextPrimary,
                        unfocusedTextColor = NocturnePalette.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                        onDismiss()
                    }
                }
            ) {
                Text(confirmLabel, color = atmosphere.primaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NocturnePalette.TextSecondary)
            }
        }
    )
}

@Composable
fun MetadataEditorDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, genre: String, year: Int?) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }
    var yearStr by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    val atmosphere = LocalArtworkAtmosphere.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.CharcoalSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = atmosphere.primaryAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Track Metadata & Specs", color = NocturnePalette.TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${song.audioQualityBadge} • ${song.channels}ch • ${TimeFormatUtils.formatFileSize(song.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = atmosphere.primaryAccent
                )
                Text(
                    text = song.filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, artist, album, genre, yearStr.toIntOrNull())
                    onDismiss()
                }
            ) {
                Text("Save", color = atmosphere.primaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NocturnePalette.TextSecondary)
            }
        }
    )
}

@Composable
fun ManualLyricsEditorDialog(
    songTitle: String,
    initialRawLrc: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialRawLrc) }
    val atmosphere = LocalArtworkAtmosphere.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.CharcoalSurface,
        title = {
            Text("Edit Lyrics — $songTitle", color = NocturnePalette.TextPrimary, maxLines = 1)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Supports standard [mm:ss.xx] LRC lines, <mm:ss.xx>word karaoke tags, or plain lyrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextSecondary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("[00:12.40]First lyric line...\n[00:16.80]Second lyric line...") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text)
                    onDismiss()
                }
            ) {
                Text("Save Lyrics", color = atmosphere.primaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NocturnePalette.TextSecondary)
            }
        }
    )
}

@Composable
fun SleepTimerBottomSheet(
    sleepTimerState: SleepTimerState,
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int, stopAtEndOfTrack: Boolean, fadeOut: Boolean) -> Unit,
    onCancelTimer: () -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var fadeOut by remember { mutableStateOf(sleepTimerState.fadeOutEnabled) }
    var stopAtTrackEnd by remember { mutableStateOf(sleepTimerState.stopAtEndOfTrack) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.ObsidianSurface,
        contentColor = NocturnePalette.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = atmosphere.primaryAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.headlineMedium)
            }

            if (sleepTimerState.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(atmosphere.primaryAccent.copy(alpha = 0.16f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (sleepTimerState.stopAtEndOfTrack && sleepTimerState.totalDurationMinutes == 0) {
                                "Stops at end of current track"
                            } else {
                                "Remaining: ${TimeFormatUtils.formatDurationMillis(sleepTimerState.remainingMs)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = NocturnePalette.TextPrimary
                        )
                        Text(
                            text = if (sleepTimerState.fadeOutEnabled) "Smooth 10s volume fade-out active" else "Immediate pause",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturnePalette.TextSecondary
                        )
                    }
                    TextButton(onClick = {
                        onCancelTimer()
                        onDismiss()
                    }) {
                        Text("Turn Off", color = NocturnePalette.CrimsonRose)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(15, 30, 45, 60, 90).forEach { mins ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NocturnePalette.CharcoalElevated)
                            .clickable {
                                onStartTimer(mins, stopAtTrackEnd, fadeOut)
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "$mins min",
                            style = MaterialTheme.typography.labelLarge,
                            color = NocturnePalette.TextPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NocturnePalette.CharcoalSurface)
                    .clickable {
                        onStartTimer(0, true, fadeOut)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Stop After Current Track Ends",
                    style = MaterialTheme.typography.labelLarge,
                    color = atmosphere.primaryAccent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gradual 10-second fade-out", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = fadeOut,
                    onCheckedChange = { fadeOut = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = atmosphere.primaryAccent)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Finish playing last track before pausing", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = stopAtTrackEnd,
                    onCheckedChange = { stopAtTrackEnd = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = atmosphere.primaryAccent)
                )
            }
        }
    }
}

@Composable
fun PlaybackSpeedBottomSheet(
    currentSpeed: Float,
    currentPitch: Float,
    onDismiss: () -> Unit,
    onApply: (speed: Float, pitch: Float) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    var pitch by remember { mutableFloatStateOf(currentPitch) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.ObsidianSurface,
        contentColor = NocturnePalette.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, tint = atmosphere.primaryAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Playback Speed & Pitch", style = MaterialTheme.typography.headlineMedium)
                }
                TextButton(onClick = {
                    speed = 1.0f
                    pitch = 1.0f
                    onApply(1.0f, 1.0f)
                }) {
                    Text("Reset 1.0x", color = atmosphere.primaryAccent)
                }
            }

            Text(
                text = String.format("Speed: %.2fx", speed),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = speed,
                onValueChange = {
                    speed = (it * 20f).roundToInt() / 20f
                    onApply(speed, pitch)
                },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = atmosphere.primaryAccent,
                    activeTrackColor = atmosphere.primaryAccent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                    val active = kotlin.math.abs(speed - preset) < 0.02f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) atmosphere.primaryAccent else NocturnePalette.CharcoalElevated
                            )
                            .clickable {
                                speed = preset
                                onApply(speed, pitch)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${preset}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) NocturnePalette.AmoledBlack else NocturnePalette.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("Pitch: %.2fx", pitch),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = (it * 20f).roundToInt() / 20f
                    onApply(speed, pitch)
                },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = atmosphere.secondaryGlow,
                    activeTrackColor = atmosphere.secondaryGlow
                )
            )
        }
    }
}

@Composable
fun EqualizerBottomSheet(
    equalizerState: EqualizerState,
    onDismiss: () -> Unit,
    onUpdateEqualizer: (
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoost: Int,
        virtualizer: Int,
        loudnessMb: Int
    ) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var enabled by remember(equalizerState.enabled) { mutableStateOf(equalizerState.enabled) }
    var currentPreset by remember(equalizerState.currentPresetName) { mutableStateOf(equalizerState.currentPresetName) }
    val bandLevels = remember(equalizerState.bands) {
        mutableStateListOf<Int>().apply {
            addAll(equalizerState.bands.map { it.levelMillibels })
        }
    }
    var bassBoost by remember(equalizerState.bassBoostStrength) { mutableIntStateOf(equalizerState.bassBoostStrength) }
    var virtualizer by remember(equalizerState.virtualizerStrength) { mutableIntStateOf(equalizerState.virtualizerStrength) }
    var loudnessMb by remember(equalizerState.loudnessGainMb) { mutableIntStateOf(equalizerState.loudnessGainMb) }

    fun pushUpdate() {
        onUpdateEqualizer(enabled, currentPreset, bandLevels.toList(), bassBoost, virtualizer, loudnessMb)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NocturnePalette.ObsidianSurface,
        contentColor = NocturnePalette.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = atmosphere.primaryAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Reference Equalizer & DSP", style = MaterialTheme.typography.headlineMedium)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        pushUpdate()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = atmosphere.primaryAccent)
                )
            }

            // Preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                equalizerState.availablePresets.forEach { preset ->
                    val selected = preset == currentPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) atmosphere.primaryAccent else NocturnePalette.CharcoalElevated
                            )
                            .clickable {
                                currentPreset = preset
                                if (preset != "Custom") {
                                    val curve = EqualizerState.presetCurveMb(preset)
                                    for (i in bandLevels.indices) {
                                        bandLevels[i] = curve.getOrElse(i) { 0 }
                                    }
                                }
                                enabled = true
                                pushUpdate()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) NocturnePalette.AmoledBlack else NocturnePalette.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            equalizerState.bands.forEachIndexed { idx, band ->
                val currentMb = bandLevels.getOrElse(idx) { 0 }
                val db = currentMb / 100f
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = band.frequencyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = NocturnePalette.TextPrimary
                        )
                        Text(
                            text = String.format("%+.1f dB", db),
                            style = MaterialTheme.typography.labelSmall,
                            color = atmosphere.primaryAccent
                        )
                    }
                    Slider(
                        value = currentMb.toFloat(),
                        onValueChange = { newVal ->
                            bandLevels[idx] = newVal.roundToInt()
                            currentPreset = "Custom"
                            pushUpdate()
                        },
                        valueRange = band.minLevelMillibels.toFloat()..band.maxLevelMillibels.toFloat(),
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = atmosphere.primaryAccent,
                            activeTrackColor = atmosphere.primaryAccent
                        )
                    )
                }
            }

            HorizontalDivider(color = NocturnePalette.GlassBorderHairline)

            Text(
                text = "Sub-Bass Harmonic Boost (${bassBoost / 10}%)",
                style = MaterialTheme.typography.titleSmall,
                color = NocturnePalette.TextPrimary
            )
            Slider(
                value = bassBoost.toFloat(),
                onValueChange = {
                    bassBoost = it.roundToInt()
                    pushUpdate()
                },
                valueRange = 0f..1000f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = atmosphere.primaryAccent,
                    activeTrackColor = atmosphere.primaryAccent
                )
            )

            Text(
                text = "Stereo Spatial Virtualizer (${virtualizer / 10}%)",
                style = MaterialTheme.typography.titleSmall,
                color = NocturnePalette.TextPrimary
            )
            Slider(
                value = virtualizer.toFloat(),
                onValueChange = {
                    virtualizer = it.roundToInt()
                    pushUpdate()
                },
                valueRange = 0f..1000f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = atmosphere.secondaryGlow,
                    activeTrackColor = atmosphere.secondaryGlow
                )
            )

            Text(
                text = String.format("Output Preamp / Loudness Gain (+%.1f dB)", loudnessMb / 100f),
                style = MaterialTheme.typography.titleSmall,
                color = NocturnePalette.TextPrimary
            )
            Slider(
                value = loudnessMb.toFloat(),
                onValueChange = {
                    loudnessMb = it.roundToInt()
                    pushUpdate()
                },
                valueRange = 0f..1200f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = NocturnePalette.WarmAmber,
                    activeTrackColor = NocturnePalette.WarmAmber
                )
            )
        }
    }
}
