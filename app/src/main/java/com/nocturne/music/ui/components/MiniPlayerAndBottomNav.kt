package com.nocturne.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.nocturne.music.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.model.PlaybackUiState

enum class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    LIBRARY("library", "Library", Icons.Filled.LibraryMusic),
    SEARCH("search", "Search", Icons.Filled.Search)
}

val MiniPlayerNavGap = 4.dp

@Composable
fun PersistentMiniPlayer(
    playbackState: PlaybackUiState,
    songColor: Color,
    onExpandNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong
    val atmosphere = LocalArtworkAtmosphere.current
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (song != null) {
            val shape = RoundedCornerShape(20.dp)
            val animatedColor by androidx.compose.animation.animateColorAsState(targetValue = songColor, animationSpec = androidx.compose.animation.core.tween(500), label = "miniPlayerColor")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = shape,
                        ambientColor = animatedColor,
                        spotColor = animatedColor
                    )
                    .clip(shape)
                    .background(NocturnePalette.ObsidianSurface)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedColor,
                                animatedColor.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height * 0.4f
                                )
                            )
                        }
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                        ),
                        shape = shape
                    )
                    .pointerInput(song.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulated += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    dragAccumulated < -90f -> onSkipNext()
                                    dragAccumulated > 90f -> onSkipPrevious()
                                }
                                dragAccumulated = 0f
                            }
                        )
                    }
                    .clickable(onClick = onExpandNowPlaying)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        artworkUri = song.albumArtUri,
                        artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                        contentDescription = song.title,
                        shape = RoundedCornerShape(11.dp),
                        seedTitle = song.album,
                        iconSize = 22.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NocturnePalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturnePalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) NocturnePalette.CrimsonRose else NocturnePalette.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(atmosphere.primaryAccent)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = NocturnePalette.AmoledBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = NocturnePalette.TextPrimary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { playbackState.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.2.dp)
                        .padding(horizontal = 6.dp),
                    color = atmosphere.primaryAccent,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun NocturneBottomNavigationBar(
    currentRoute: String,
    onNavigate: (PrimaryDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = MiniPlayerNavGap, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryDestination.entries.forEach { dest ->
                val selected = currentRoute.startsWith(dest.route)

                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigate(dest) }
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (dest) {
                        PrimaryDestination.HOME -> {
                            androidx.compose.animation.Crossfade(targetState = selected, animationSpec = androidx.compose.animation.core.tween(120), label = "homeIconCrossfade") { isSelected ->
                                Icon(
                                    painter = painterResource(id = if (isSelected) R.drawable.ic_home_filled_custom else R.drawable.ic_home_custom),
                                    contentDescription = dest.label,
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        PrimaryDestination.LIBRARY -> {
                            androidx.compose.animation.Crossfade(targetState = selected, animationSpec = androidx.compose.animation.core.tween(120), label = "libraryIconCrossfade") { isSelected ->
                                Icon(
                                    painter = painterResource(id = if (isSelected) R.drawable.ic_library_filled_custom else R.drawable.ic_library_custom),
                                    contentDescription = dest.label,
                                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        else -> {
                            val tintColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                                animationSpec = androidx.compose.animation.core.tween(120),
                                label = "searchTint"
                            )
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label,
                                tint = tintColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
