package com.nocturne.music.ui.nowplaying

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.artworkAmbientCanvas
import com.nocturne.music.designsystem.liquidGlassPanel
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.PlaybackUiState
import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils
import com.nocturne.music.model.UserSettings
import com.nocturne.music.ui.components.ArtworkImage
import com.nocturne.music.ui.components.NowPlayingEqualizerBars
import com.nocturne.music.ui.lyrics.SynchronizedLyricsView
import kotlin.math.abs
import kotlin.math.roundToLong

enum class NowPlayingPane(val label: String) {
    ARTWORK("Artwork"),
    LYRICS("Lyrics"),
    QUEUE("Queue")
}

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    lyricsDocument: LyricsDocument,
    isLoadingLyrics: Boolean,
    userSettings: UserSettings,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSetPlaybackMode: (Boolean, RepeatModeState) -> Unit,
    onPostStatusMessage: (String) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onOpenTrackContext: (Song) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenPlaybackSpeed: () -> Unit,
    onEditArtworkClick: () -> Unit,
    onImportLrcClick: () -> Unit,
    onEditLyricsClick: () -> Unit,
    onFetchOnlineLyricsClick: () -> Unit,
    onAdjustLyricsOffsetMs: (Long) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    val atmosphere = LocalArtworkAtmosphere.current
    var activePane by remember { mutableStateOf(NowPlayingPane.ARTWORK) }

    val context = LocalContext.current
    DisposableEffect(activePane, userSettings.keepScreenOnDuringLyrics) {
        val window = (context as? ComponentActivity)?.window
        if (activePane == NowPlayingPane.LYRICS && userSettings.keepScreenOnDuringLyrics) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val displayedFraction = if (isScrubbing) scrubFraction else playbackState.progressFraction
    val displayedPositionMs = if (isScrubbing) {
        (scrubFraction * playbackState.durationMs).roundToLong()
    } else {
        playbackState.positionMs
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .artworkAmbientCanvas(
                atmosphere = atmosphere,
                style = userSettings.dynamicBackgroundStyle,
                intensity = 1.0f
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp), // 16dp horizontal padding so artwork has 16dp margins
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: collapse handle, pane switcher pill, three-dot menu
            NowPlayingTopBar(
                activePane = activePane,
                onSelectPane = { activePane = it },
                playbackSpeed = playbackState.playbackSpeed,
                onCollapse = onCollapse,
                onOpenPlaybackSpeed = onOpenPlaybackSpeed,
                onOpenEqualizer = onOpenEqualizer,
                onEditArtworkClick = onEditArtworkClick,
                onMoreClick = { onOpenTrackContext(song) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Centerpiece stage (Artwork / Synchronized Lyrics / Interactive Queue)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activePane,
                    transitionSpec = {
                        fadeIn(tween(260)) togetherWith fadeOut(tween(200))
                    },
                    label = "nowPlayingPaneTransition"
                ) { pane ->
                    when (pane) {
                        NowPlayingPane.ARTWORK -> {
                            ArtworkShowcaseStage(
                                song = song,
                                isPlaying = playbackState.isPlaying,
                                smoothTransitions = userSettings.smoothArtworkTransitions,
                                onSwipeNext = onSkipNext,
                                onSwipePrevious = onSkipPrevious,
                                onSwipeDownCollapse = onCollapse,
                                onTapLyrics = { activePane = NowPlayingPane.LYRICS }
                            )
                        }

                        NowPlayingPane.LYRICS -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CompactNowPlayingTrackHeader(
                                    song = song,
                                    onClickArtwork = { activePane = NowPlayingPane.ARTWORK }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                SynchronizedLyricsView(
                                    lyricsDocument = lyricsDocument,
                                    isLoading = isLoadingLyrics,
                                    currentPositionMs = playbackState.positionMs,
                                    fontSizePreference = userSettings.lyricsFontSize,
                                    wordLevelKaraokeEnabled = userSettings.wordLevelKaraokeHighlight,
                                    onSeekToLine = onSeekTo,
                                    onImportLrcClick = onImportLrcClick,
                                    onEditLyricsClick = onEditLyricsClick,
                                    onFetchOnlineClick = onFetchOnlineLyricsClick,
                                    onAdjustOffsetMs = onAdjustLyricsOffsetMs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        NowPlayingPane.QUEUE -> {
                            NowPlayingQueueStage(
                                queue = playbackState.queue,
                                currentIndex = playbackState.currentQueueIndex,
                                isPlaying = playbackState.isPlaying,
                                onSelectQueueIndex = onSelectQueueIndex,
                                onMoveQueueItem = onMoveQueueItem,
                                onRemoveQueueItem = onRemoveQueueItem
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Track Metadata & Favorite Row (Fixed height container: 56.dp, single-line marquee, explicit theme white colors, no edge fade)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    key(song.id) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = NocturnePalette.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 1500,
                                    velocity = 40.dp
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    key(song.id) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    initialDelayMillis = 1500,
                                    velocity = 40.dp
                                )
                        ) {
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NocturnePalette.TextSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.clickable {
                                    onCollapse()
                                    onNavigateToArtist(song.artistId)
                                }
                            )
                            if (song.album.isNotBlank()) {
                                Text(
                                    text = "  •  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NocturnePalette.TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = song.album,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NocturnePalette.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.clickable {
                                        onCollapse()
                                        onNavigateToAlbum(song.albumId)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (song.isFavorite) NocturnePalette.CrimsonRose else NocturnePalette.TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Technical Audio Spec & Active Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NocturnePalette.GlassSurfaceMedium)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = song.audioQualityBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturnePalette.TextSecondary
                    )
                }

                if (userSettings.crossfadeDurationSec > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Crossfade ${userSettings.crossfadeDurationSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (playbackState.sleepTimer.isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NocturnePalette.WarmAmber.copy(alpha = 0.18f))
                            .clickable(onClick = onOpenSleepTimer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Sleep ${TimeFormatUtils.formatDurationMillis(playbackState.sleepTimer.remainingMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturnePalette.WarmAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Scrub Bar & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                var barWidth by remember { mutableStateOf(0) }

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
                    // Inactive Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    )

                    // Active Track (always white)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(displayedFraction.coerceIn(0f, 1f))
                            .height(3.dp)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )

                    // Thumb (solid white circle 14dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Transport Controls: [Shuffle/Repeat] [Previous] [Play/Pause] [Next] [Sleep]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Merged Shuffle/Repeat Button (48dp touch target)
                IconButton(
                    onClick = {
                        when {
                            !playbackState.shuffleEnabled && playbackState.repeatMode == RepeatModeState.OFF -> {
                                onSetPlaybackMode(false, RepeatModeState.ALL)
                                onPostStatusMessage("Repeat all")
                            }
                            !playbackState.shuffleEnabled && playbackState.repeatMode == RepeatModeState.ALL -> {
                                onSetPlaybackMode(false, RepeatModeState.ONE)
                                onPostStatusMessage("Repeat one")
                            }
                            !playbackState.shuffleEnabled && playbackState.repeatMode == RepeatModeState.ONE -> {
                                onSetPlaybackMode(true, RepeatModeState.OFF)
                                onPostStatusMessage("Shuffle on")
                            }
                            else -> {
                                onSetPlaybackMode(false, RepeatModeState.OFF)
                                onPostStatusMessage("Sequential")
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    val modeState = when {
                        playbackState.shuffleEnabled -> 3 // Shuffle
                        playbackState.repeatMode == RepeatModeState.ALL -> 1 // Repeat All
                        playbackState.repeatMode == RepeatModeState.ONE -> 2 // Repeat One
                        else -> 0 // Off
                    }
                    Crossfade(targetState = modeState, label = "playbackModeCrossfade") { mode ->
                        val vector = when (mode) {
                            1 -> Icons.Rounded.Repeat
                            2 -> Icons.Rounded.RepeatOne
                            3 -> Icons.Rounded.Shuffle
                            else -> Icons.Rounded.Repeat
                        }
                        val isActive = mode > 0
                        val tint = if (isActive) Color.White else NocturnePalette.TextSecondary
                        Icon(
                            imageVector = vector,
                            contentDescription = "Playback Mode",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Previous Track (icon size 38dp, touch target 56dp)
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = NocturnePalette.TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // 3. Play / Pause (circle diameter 68dp, icon size 34dp)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Color.White.copy(alpha = 0.2f),
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

                // 4. Next Track (icon size 38dp, touch target 56dp)
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = NocturnePalette.TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // 5. Sleep Button (right of Next, 48dp touch target)
                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isSleepActive = playbackState.sleepTimer.isActive
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (isSleepActive) Color.White else NocturnePalette.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isSleepActive) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = TimeFormatUtils.formatDurationMillis(playbackState.sleepTimer.remainingMs),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    activePane: NowPlayingPane,
    onSelectPane: (NowPlayingPane) -> Unit,
    playbackSpeed: Float,
    onCollapse: () -> Unit,
    onOpenPlaybackSpeed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onEditArtworkClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
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

        // Segmented Liquid-Glass Mode Switcher
        Row(
            modifier = Modifier
                .liquidGlassPanel(
                    shape = RoundedCornerShape(50),
                    tintColor = Color.White,
                    fillAlpha = 0.12f,
                    borderAlpha = 0.24f
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NowPlayingPane.entries.forEach { pane ->
                val selected = pane == activePane
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) Color.White else Color.Transparent
                        )
                        .clickable { onSelectPane(pane) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = pane.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) NocturnePalette.AmoledBlack else NocturnePalette.TextSecondary
                    )
                }
            }
        }

        // Three-dot menu button (top right)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
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
                    text = { Text("Queue") },
                    onClick = {
                        menuExpanded = false
                        onSelectPane(NowPlayingPane.QUEUE)
                    }
                )
                DropdownMenuItem(
                    text = { Text(String.format("Playback speed (%.1fx)", playbackSpeed)) },
                    onClick = {
                        menuExpanded = false
                        onOpenPlaybackSpeed()
                    }
                )
                DropdownMenuItem(
                    text = { Text("DSP / EQ") },
                    onClick = {
                        menuExpanded = false
                        onOpenEqualizer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit artwork") },
                    onClick = {
                        menuExpanded = false
                        onEditArtworkClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Track Options") },
                    onClick = {
                        menuExpanded = false
                        onMoreClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ArtworkShowcaseStage(
    song: Song,
    isPlaying: Boolean,
    smoothTransitions: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeDownCollapse: () -> Unit,
    onTapLyrics: () -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artworkScale"
    )

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Exactly 16dp horizontal margins
            .aspectRatio(1f)
            .scale(artworkScale)
            .pointerInput(song.id) {
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(dragX) > abs(dragY)) {
                            if (dragX < -110f) onSwipeNext()
                            else if (dragX > 110f) onSwipePrevious()
                        } else if (dragY > 140f) {
                            onSwipeDownCollapse()
                        }
                    }
                )
            }
            .clickable(onClick = onTapLyrics),
        contentAlignment = Alignment.Center
    ) {
        val artShape = RoundedCornerShape(14.dp)
        if (smoothTransitions) {
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    fadeIn(tween(380)) togetherWith fadeOut(tween(280))
                },
                label = "artworkCrossfade"
            ) { targetSong ->
                ArtworkImage(
                    artworkUri = targetSong.albumArtUri,
                    artworkKey = "${targetSong.id}_${targetSong.customArtworkUpdatedAtMs}",
                    contentDescription = targetSong.title,
                    shape = artShape,
                    seedTitle = targetSong.album,
                    iconSize = 84.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 20.dp,
                            shape = artShape,
                            ambientColor = Color.White.copy(alpha = 0.15f),
                            spotColor = Color.White.copy(alpha = 0.15f)
                        )
                )
            }
        } else {
            ArtworkImage(
                artworkUri = song.albumArtUri,
                artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                contentDescription = song.title,
                shape = artShape,
                seedTitle = song.album,
                iconSize = 84.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 20.dp,
                        shape = artShape,
                        ambientColor = Color.White.copy(alpha = 0.15f),
                        spotColor = Color.White.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

@Composable
private fun CompactNowPlayingTrackHeader(
    song: Song,
    onClickArtwork: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NocturnePalette.GlassSurfaceSubtle)
            .clickable(onClick = onClickArtwork)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUri = song.albumArtUri,
            artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
            contentDescription = song.title,
            shape = RoundedCornerShape(10.dp),
            seedTitle = song.album,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = NocturnePalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • Tap to return to artwork",
                style = MaterialTheme.typography.bodySmall,
                color = NocturnePalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NowPlayingQueueStage(
    queue: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassPanel(
                shape = RoundedCornerShape(20.dp),
                tintColor = Color.White,
                fillAlpha = 0.10f,
                borderAlpha = 0.20f
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playback Queue (${queue.size} tracks)",
                style = MaterialTheme.typography.titleMedium,
                color = NocturnePalette.TextPrimary
            )
            if (currentIndex >= 0) {
                Text(
                    text = "Playing #$currentIndex",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = queue,
                key = { idx, item -> "${item.id}_$idx" }
            ) { index, item ->
                val isCurrent = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) Color.White.copy(alpha = 0.16f) else Color.Transparent
                        )
                        .clickable { onSelectQueueIndex(index) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) {
                        NowPlayingEqualizerBars(
                            isPlaying = isPlaying,
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NocturnePalette.TextMuted,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCurrent) Color.White else NocturnePalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturnePalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index > 0) {
                        IconButton(
                            onClick = { onMoveQueueItem(index, index - 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Up", tint = NocturnePalette.TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < queue.lastIndex) {
                        IconButton(
                            onClick = { onMoveQueueItem(index, index + 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Down", tint = NocturnePalette.TextMuted, modifier = Modifier.size(18.dp).scale(scaleX = 1f, scaleY = -1f))
                        }
                    }
                    IconButton(
                        onClick = { onRemoveQueueItem(index) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Remove from Queue", tint = NocturnePalette.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
