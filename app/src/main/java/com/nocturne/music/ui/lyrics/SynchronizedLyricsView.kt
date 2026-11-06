package com.nocturne.music.ui.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.liquidGlassPanel
import com.nocturne.music.model.LyricsDocument
import com.nocturne.music.model.LyricsFontSize
import com.nocturne.music.model.LyricsLine
import com.nocturne.music.model.LyricsSource
import com.nocturne.music.model.TimeFormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Centerpiece Synchronized Lyrics component supporting:
 * - Real-time line highlighting & smooth centered auto-scroll
 * - Word-level karaoke illumination when word timestamps exist
 * - Non-fighting manual scroll detection with a floating "Resync" pill
 * - Tap-to-seek on any lyric line
 * - Live offset calibration (-100ms / +100ms)
 * - Clean fallback when lyrics are unavailable + local .LRC import & manual editor triggers
 */
@Composable
fun SynchronizedLyricsView(
    lyricsDocument: LyricsDocument,
    isLoading: Boolean,
    currentPositionMs: Long,
    fontSizePreference: LyricsFontSize,
    wordLevelKaraokeEnabled: Boolean,
    onSeekToLine: (Long) -> Unit,
    onImportLrcClick: () -> Unit,
    onEditLyricsClick: () -> Unit,
    onFetchOnlineClick: () -> Unit,
    onAdjustOffsetMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val activeLineIndex = remember(lyricsDocument, currentPositionMs) {
        lyricsDocument.findActiveLineIndex(currentPositionMs)
    }

    var userManualScrollActive by remember { mutableStateOf(false) }
    var lastManualInteractionMs by remember { mutableLongStateOf(0L) }

    // Detect user manual scroll so automatic scrolling never fights the user's finger
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userManualScrollActive = true
            lastManualInteractionMs = System.currentTimeMillis()
        } else if (userManualScrollActive) {
            delay(4200L)
            if (System.currentTimeMillis() - lastManualInteractionMs >= 4000L) {
                userManualScrollActive = false
            }
        }
    }

    // Smooth auto-scroll to active line when not manually scrolling
    LaunchedEffect(activeLineIndex, userManualScrollActive, lyricsDocument.songId, listState.layoutInfo.viewportSize) {
        if (!userManualScrollActive && lyricsDocument.isSynced && activeLineIndex >= 0) {
            runCatching {
                // Calculate an appropriate scroll offset to try and center the item vertically.
                // We don't have the exact item height before layout, but a rough estimate or 0 often works
                // with animateScrollToItem if we want it at the top, or negative offset to push it down.
                // A better approach to center is to use listState.layoutInfo.viewportSize.height
                val viewportHeight = listState.layoutInfo.viewportSize.height
                // Assuming average line height is around 100-200px. Let's aim to place the active line
                // around the middle of the viewport.
                val targetOffset = -(viewportHeight / 2 - 100) // approx center
                listState.animateScrollToItem(
                    index = activeLineIndex,
                    scrollOffset = targetOffset
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = atmosphere.primaryAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scanning local .LRC & embedded tags…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NocturnePalette.TextSecondary
                        )
                    }
                }
            }

            lyricsDocument.lines.isEmpty() -> {
                EmptyLyricsFallbackCard(
                    onImportLrcClick = onImportLrcClick,
                    onEditLyricsClick = onEditLyricsClick,
                    onFetchOnlineClick = onFetchOnlineClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Source badge & sync offset toolbar
                    LyricsMetadataToolbar(
                        lyricsDocument = lyricsDocument,
                        onImportLrcClick = onImportLrcClick,
                        onEditLyricsClick = onEditLyricsClick,
                        onAdjustOffsetMs = onAdjustOffsetMs
                    )

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 22.dp,
                            end = 22.dp,
                            top = 36.dp,
                            bottom = 160.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = lyricsDocument.lines,
                            key = { idx, line -> "${line.startTimeMs}_$idx" }
                        ) { index, line ->
                            val isCurrent = lyricsDocument.isSynced && index == activeLineIndex
                            val isPassed = lyricsDocument.isSynced && activeLineIndex > index

                            LyricsLineItem(
                                line = line,
                                isSynced = lyricsDocument.isSynced,
                                isCurrent = isCurrent,
                                isPassed = isPassed,
                                adjustedPositionMs = (currentPositionMs - lyricsDocument.offsetMs).coerceAtLeast(0L),
                                fontSizePreference = fontSizePreference,
                                wordLevelKaraokeEnabled = wordLevelKaraokeEnabled,
                                accentColor = atmosphere.primaryAccent,
                                onClick = {
                                    if (lyricsDocument.isSynced) {
                                        userManualScrollActive = false
                                        val seekTarget = (line.startTimeMs + lyricsDocument.offsetMs).coerceAtLeast(0L)
                                        onSeekToLine(seekTarget)
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating "Resync" pill when user manually scrolls away from active line
                AnimatedVisibility(
                    visible = userManualScrollActive && lyricsDocument.isSynced && activeLineIndex >= 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .liquidGlassPanel(
                                shape = RoundedCornerShape(50),
                                tintColor = atmosphere.primaryAccent,
                                fillAlpha = 0.24f,
                                borderAlpha = 0.42f
                            )
                            .clickable {
                                userManualScrollActive = false
                                coroutineScope.launch {
                                    if (activeLineIndex >= 0) {
                                        listState.animateScrollToItem(activeLineIndex, -220)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Resync Lyrics",
                            tint = atmosphere.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Resync to ${TimeFormatUtils.formatDurationMillis(currentPositionMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NocturnePalette.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsMetadataToolbar(
    lyricsDocument: LyricsDocument,
    onImportLrcClick: () -> Unit,
    onEditLyricsClick: () -> Unit,
    onAdjustOffsetMs: (Long) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(atmosphere.primaryAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val badge = buildString {
                    append(lyricsDocument.source.label)
                    if (lyricsDocument.hasWordSync) append(" • Word Karaoke")
                    else if (lyricsDocument.isSynced) append(" • Synced")
                }
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = atmosphere.primaryAccent
                )
            }

            if (lyricsDocument.isSynced && lyricsDocument.offsetMs != 0L) {
                val sign = if (lyricsDocument.offsetMs > 0) "+" else ""
                Text(
                    text = "${sign}${lyricsDocument.offsetMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (lyricsDocument.isSynced) {
                IconButton(
                    onClick = { onAdjustOffsetMs(-100L) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Lyrics Earlier (-100ms)",
                        tint = NocturnePalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onAdjustOffsetMs(100L) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Lyrics Later (+100ms)",
                        tint = NocturnePalette.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(
                onClick = onImportLrcClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileOpen,
                    contentDescription = "Import .LRC File",
                    tint = NocturnePalette.TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(
                onClick = onEditLyricsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = "Edit Lyrics",
                    tint = NocturnePalette.TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineItem(
    line: LyricsLine,
    isSynced: Boolean,
    isCurrent: Boolean,
    isPassed: Boolean,
    adjustedPositionMs: Long,
    fontSizePreference: LyricsFontSize,
    wordLevelKaraokeEnabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.03f else 0.97f,
        animationSpec = tween(durationMillis = 280),
        label = "lyricScale"
    )
    val lineAlpha by animateFloatAsState(
        targetValue = when {
            !isSynced -> 0.90f
            isCurrent -> 1.0f
            isPassed -> 0.34f
            else -> 0.56f
        },
        animationSpec = tween(durationMillis = 260),
        label = "lyricAlpha"
    )
    val lineColor by animateColorAsState(
        targetValue = when {
            !isSynced -> NocturnePalette.TextPrimary
            isCurrent -> NocturnePalette.TextPrimary
            isPassed -> NocturnePalette.TextMuted
            else -> NocturnePalette.TextSecondary
        },
        animationSpec = tween(durationMillis = 260),
        label = "lyricColor"
    )

    val fontSizeSp = if (isCurrent || !isSynced) {
        fontSizePreference.activeSp.sp
    } else {
        fontSizePreference.inactiveSp.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(lineAlpha)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) accentColor.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable(enabled = isSynced, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (isCurrent && wordLevelKaraokeEnabled && line.hasWordTimestamps) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                line.words.forEach { word ->
                    val wordDuration = (word.endTimeMs - word.startTimeMs).coerceAtLeast(1L)
                    val wordProgress = ((adjustedPositionMs - word.startTimeMs).toFloat() / wordDuration.toFloat())
                        .coerceIn(0f, 1f)
                    val wordColor = when {
                        wordProgress >= 1f -> NocturnePalette.TextPrimary
                        wordProgress > 0f -> accentColor
                        else -> NocturnePalette.TextSecondary.copy(alpha = 0.55f)
                    }
                    Text(
                        text = word.text.trim(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSizeSp,
                            lineHeight = (fontSizeSp.value * 1.32f).sp,
                            fontWeight = if (wordProgress > 0f) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = wordColor
                    )
                }
            }
        } else {
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSizeSp,
                    lineHeight = (fontSizeSp.value * 1.32f).sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = lineColor
            )
        }

        if (!line.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = line.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) accentColor.copy(alpha = 0.9f) else NocturnePalette.TextMuted
            )
        }

        if (isSynced && isCurrent) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = TimeFormatUtils.formatDurationMillis(line.startTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun EmptyLyricsFallbackCard(
    onImportLrcClick: () -> Unit,
    onEditLyricsClick: () -> Unit,
    onFetchOnlineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .liquidGlassPanel(
                shape = shape,
                tintColor = atmosphere.primaryAccent,
                fillAlpha = 0.12f,
                borderAlpha = 0.25f
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Lyrics,
            contentDescription = null,
            tint = atmosphere.primaryAccent,
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Synchronized Lyrics Found",
            style = MaterialTheme.typography.titleLarge,
            color = NocturnePalette.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Import a local .lrc file, paste LRC timestamps manually, or fetch and cache synchronized lyrics for offline playback.",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturnePalette.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(atmosphere.primaryAccent)
                    .clickable(onClick = onImportLrcClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = null,
                        tint = NocturnePalette.AmoledBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import .LRC",
                        style = MaterialTheme.typography.labelLarge,
                        color = NocturnePalette.AmoledBlack
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NocturnePalette.CharcoalElevated)
                    .clickable(onClick = onEditLyricsClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = NocturnePalette.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Paste LRC",
                        style = MaterialTheme.typography.labelLarge,
                        color = NocturnePalette.TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NocturnePalette.GlassSurfaceMedium)
                .clickable(onClick = onFetchOnlineClick)
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = atmosphere.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search LRCLIB & Cache Offline",
                    style = MaterialTheme.typography.labelLarge,
                    color = NocturnePalette.TextPrimary
                )
            }
        }
    }
}
