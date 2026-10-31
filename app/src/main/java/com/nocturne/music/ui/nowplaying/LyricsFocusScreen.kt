package com.nocturne.music.ui.nowplaying

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.PlaybackUiState
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils
import kotlin.math.roundToLong

@Composable
fun LyricsFocusScreen(
    songColor: Color,
    playbackState: PlaybackUiState,
    lyricsDocument: LyricsDocument,
    isLoadingLyrics: Boolean,
    onCollapse: () -> Unit,
    onExitLyricsMode: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onImportLrcClick: () -> Unit,
    onFetchOnlineClick: () -> Unit,
    onOpenTrackContext: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = songColor,
        animationSpec = tween(500),
        label = "npFocusBgColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NocturnePalette.AmoledBlack)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Fixed, opaque dynamic background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedColor,
                            NocturnePalette.AmoledBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top bar: chevron only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = NocturnePalette.TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // 2. Title & Artist, centered, with 56dp side margins
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NocturnePalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturnePalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Lyrics Area (takes all remaining middle space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                InteractiveLyricsStage(
                    song = song,
                    lyricsDocument = lyricsDocument,
                    isLoading = isLoadingLyrics,
                    currentPositionMs = playbackState.positionMs,
                    onSeekToLine = onSeekTo,
                    onImportLrcClick = onImportLrcClick,
                    onFetchOnlineClick = onFetchOnlineClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Row above seek bar: Exit button (left) and 3-dot (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onExitLyricsMode)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exit lyrics",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options Menu",
                            tint = NocturnePalette.TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Song details") },
                            onClick = {
                                menuExpanded = false
                                onOpenTrackContext(song)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Seek bar & time labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                var barWidth by remember { mutableStateOf(0) }
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubFraction by remember { mutableFloatStateOf(0f) }

                val displayedFraction = if (isScrubbing) scrubFraction else playbackState.progressFraction
                val displayedPositionMs = if (isScrubbing) {
                    (scrubFraction * playbackState.durationMs).roundToLong()
                } else {
                    playbackState.positionMs
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .onSizeChanged { barWidth = it.width }
                        .pointerInput(playbackState.durationMs) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (barWidth > 0) {
                                        val fraction = (offset.x / barWidth).coerceIn(0f, 1f)
                                        val targetMs = (fraction * playbackState.durationMs).roundToLong()
                                        onSeekTo(targetMs)
                                    }
                                }
                            )
                        }
                        .pointerInput(playbackState.durationMs) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true
                                    if (barWidth > 0) {
                                        scrubFraction = (offset.x / barWidth).coerceIn(0f, 1f)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (barWidth > 0) {
                                        scrubFraction = (change.position.x / barWidth).coerceIn(0f, 1f)
                                    }
                                },
                                onDragEnd = {
                                    val targetMs = (scrubFraction * playbackState.durationMs).roundToLong()
                                    isScrubbing = false
                                    onSeekTo(targetMs)
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(displayedFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(displayedFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeFormatUtils.formatDurationMillis(displayedPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isScrubbing) Color.White else NocturnePalette.TextSecondary
                    )
                    Text(
                        text = TimeFormatUtils.formatDurationMillis((playbackState.durationMs - displayedPositionMs).coerceAtLeast(0L)),
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturnePalette.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Play/Pause button only, centered
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = Color.White.copy(alpha = 0.2f)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or Pause",
                    tint = NocturnePalette.AmoledBlack,
                    modifier = Modifier.size(34.dp)
                )
            }

            // 7. Spacing under the play button
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
