package com.nocturne.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.liquidGlassPanel
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.Album
import com.nocturne.music.model.Artist
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = NocturnePalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(atmosphere.primaryAccent.copy(alpha = 0.14f))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = atmosphere.primaryAccent
                )
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackIndexNumber: Int? = null
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val rowShape = RoundedCornerShape(14.dp)

    val rowModifier = if (isCurrentSong) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .liquidGlassPanel(
                shape = rowShape,
                tintColor = atmosphere.primaryAccent,
                fillAlpha = 0.14f,
                borderAlpha = 0.30f
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trackIndexNumber != null) {
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isCurrentSong) {
                    NowPlayingEqualizerBars(
                        isPlaying = isPlaying,
                        color = atmosphere.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = trackIndexNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = NocturnePalette.TextMuted
                    )
                }
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                ArtworkImage(
                    artworkUri = song.albumArtUri,
                    artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                    contentDescription = song.title,
                    shape = RoundedCornerShape(10.dp),
                    seedTitle = song.album,
                    iconSize = 22.dp,
                    modifier = Modifier.size(50.dp)
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NowPlayingEqualizerBars(
                            isPlaying = isPlaying,
                            color = atmosphere.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isCurrentSong) atmosphere.primaryAccent else NocturnePalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NocturnePalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = song.audioQualityBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextMuted
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextMuted
                )
                Text(
                    text = TimeFormatUtils.formatDurationMillis(song.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextSecondary
                )
            }
        }

        FavoriteButton(
            isFavorite = song.isFavorite,
            onClick = onFavoriteClick,
            iconSize = 19.dp,
            buttonSize = 36.dp,
            unfavoriteColor = NocturnePalette.TextMuted,
            modifier = Modifier
        )

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Track Options",
                tint = NocturnePalette.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NowPlayingEqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eqBars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.95f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 0.25f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = if (isPlaying) 1.0f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun AlbumArtworkCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .background(color = NocturnePalette.ObsidianSurface, shape = cardShape)
            .border(width = 0.6.dp, color = NocturnePalette.GlassBorderHairline, shape = cardShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            ArtworkImage(
                artworkUri = album.albumArtUri,
                contentDescription = album.title,
                shape = RoundedCornerShape(12.dp),
                seedTitle = album.title,
                iconSize = 42.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            color = NocturnePalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (album.year > 0) "${album.year} • ${album.songCount} tracks" else "${album.songCount} tracks",
            style = MaterialTheme.typography.labelSmall,
            color = NocturnePalette.TextMuted,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ArtistArtworkCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ArtworkImage(
            artworkUri = artist.artworkUri,
            contentDescription = artist.name,
            shape = CircleShape,
            seedTitle = artist.name,
            iconSize = 36.dp,
            modifier = Modifier.size(104.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            color = NocturnePalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${artist.albumCount} albums • ${artist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePalette.TextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .matteSurface(shape = shape, surfaceColor = NocturnePalette.ObsidianSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUri = playlist.coverArtUri,
            contentDescription = playlist.name,
            shape = RoundedCornerShape(12.dp),
            seedTitle = playlist.name,
            iconSize = 28.dp,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = NocturnePalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(playlist.totalDurationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = NocturnePalette.TextMuted
            )
        }
        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play Playlist",
                tint = NocturnePalette.TextPrimary
            )
        }
    }
}
