package com.nocturne.music.ui.nowplaying

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.PlaybackUiState
import com.nocturne.music.model.RepeatModeState
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils
import com.nocturne.music.model.UserSettings
import com.nocturne.music.ui.components.ArtworkImage
import com.nocturne.music.ui.components.NowPlayingEqualizerBars
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun NowPlayingScreen(
    songColor: Color,
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
    val listState = rememberLazyListState()
    var showQueueSheet by remember { mutableStateOf(false) }

    // Return page scroll to top when switching songs
    LaunchedEffect(song.id) {
        listState.scrollToItem(0)
    }

    val context = LocalContext.current
    DisposableEffect(userSettings.keepScreenOnDuringLyrics) {
        val window = (context as? ComponentActivity)?.window
        if (userSettings.keepScreenOnDuringLyrics) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = songColor,
        animationSpec = tween(500),
        label = "npBgColor"
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
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
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
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pinned top bar (does not scroll)
            NowPlayingTopBar(
                playbackSpeed = playbackState.playbackSpeed,
                onCollapse = onCollapse,
                onOpenPlaybackSpeed = onOpenPlaybackSpeed,
                onOpenEqualizer = onOpenEqualizer,
                onEditArtworkClick = onEditArtworkClick,
                onOpenQueue = { showQueueSheet = true },
                onMoreClick = { onOpenTrackContext(song) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Scrollable content area below top bar
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. First screenful: Player block
                item(key = "player_block") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Artwork stage (static on play/pause, crossfade on song change)
                        ArtworkShowcaseStage(
                            song = song,
                            smoothTransitions = userSettings.smoothArtworkTransitions,
                            onSwipeNext = onSkipNext,
                            onSwipePrevious = onSkipPrevious,
                            onSwipeDownCollapse = onCollapse
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Track Metadata & Favorite Row
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

                        Spacer(modifier = Modifier.height(4.dp))

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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress Scrub Bar & Timestamps
                        Column(modifier = Modifier.fillMaxWidth()) {
                            var barWidth by remember { mutableStateOf(0) }
                            val displayedFraction = playbackState.progressFraction
                            val displayedPositionMs = playbackState.positionMs

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
                                    color = NocturnePalette.TextSecondary
                                )
                                Text(
                                    text = TimeFormatUtils.formatDurationMillis((playbackState.durationMs - displayedPositionMs).coerceAtLeast(0L)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocturnePalette.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Primary Transport Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    playbackState.shuffleEnabled -> 3
                                    playbackState.repeatMode == RepeatModeState.ALL -> 1
                                    playbackState.repeatMode == RepeatModeState.ONE -> 2
                                    else -> 0
                                }
                                Icon(
                                    imageVector = when (modeState) {
                                        1 -> Icons.Rounded.Repeat
                                        2 -> Icons.Rounded.RepeatOne
                                        3 -> Icons.Rounded.Shuffle
                                        else -> Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Playback Mode",
                                    tint = if (modeState > 0) Color.White else NocturnePalette.TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

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
                    }
                }

                // 2. Spacer 24dp below control row
                item(key = "spacer_lyrics") {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 3. Lyrics Card (with 20dp corner radius, white 10% fill, peeking in at bottom)
                item(key = "lyrics_card") {
                    LyricsCardSection(
                        lyricsDocument = lyricsDocument,
                        isLoading = isLoadingLyrics,
                        currentPositionMs = playbackState.positionMs,
                        onSeekToLine = onSeekTo,
                        onImportLrcClick = onImportLrcClick,
                        onFetchOnlineClick = onFetchOnlineLyricsClick
                    )
                }

                // 4. Bottom padding so last line scrolls clear of system navigation area
                item(key = "bottom_padding") {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }

        // Queue Bottom Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = NocturnePalette.ObsidianSurface,
                contentColor = NocturnePalette.TextPrimary
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .padding(16.dp)
                ) {
                    NowPlayingQueueStage(
                        queue = playbackState.queue,
                        currentIndex = playbackState.currentQueueIndex,
                        isPlaying = playbackState.isPlaying,
                        onSelectQueueIndex = {
                            onSelectQueueIndex(it)
                            showQueueSheet = false
                        },
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    playbackSpeed: Float,
    onCollapse: () -> Unit,
    onOpenPlaybackSpeed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onEditArtworkClick: () -> Unit,
    onOpenQueue: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
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
                        onOpenQueue()
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
            }
        }
    }
}

@Composable
private fun LyricsCardSection(
    lyricsDocument: LyricsDocument,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeekToLine: (Long) -> Unit,
    onImportLrcClick: () -> Unit,
    onFetchOnlineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLineIndex = remember(lyricsDocument, currentPositionMs) {
        lyricsDocument.findActiveLineIndex(currentPositionMs)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NocturnePalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                lyricsDocument.isSynced && lyricsDocument.lines.isNotEmpty() -> {
                    lyricsDocument.lines.forEachIndexed { index, line ->
                        val isActive = index == activeLineIndex
                        Text(
                            text = line.text.ifBlank { " " },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            ),
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeekToLine(line.startTimeMs) }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                !lyricsDocument.isSynced && lyricsDocument.rawText.isNotBlank() -> {
                    Text(
                        text = lyricsDocument.rawText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No lyrics found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NocturnePalette.TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable(onClick = onFetchOnlineClick)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Search Online",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable(onClick = onImportLrcClick)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Import .LRC",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkShowcaseStage(
    song: Song,
    smoothTransitions: Boolean,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeDownCollapse: () -> Unit
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
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
            },
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
                    iconSize = 56.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            ArtworkImage(
                artworkUri = song.albumArtUri,
                artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                contentDescription = song.title,
                shape = artShape,
                seedTitle = song.album,
                iconSize = 56.dp,
                modifier = Modifier.fillMaxSize()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
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
