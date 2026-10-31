package com.nocturne.music.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.ListeningHistoryEntry
import com.nocturne.music.model.Song
import com.nocturne.music.model.TimeFormatUtils
import com.nocturne.music.ui.components.ArtworkImage

@Composable
fun RecentsScreen(
    listeningHistory: List<ListeningHistoryEntry>,
    onPlaySong: (Song) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 160.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .matteSurface(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = NocturnePalette.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Recents",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = NocturnePalette.TextPrimary
            )
        }

        if (listeningHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No listening history yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NocturnePalette.TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listeningHistory, key = { "recent_hist_${it.historyId}" }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matteSurface(RoundedCornerShape(12.dp))
                            .clickable { onPlaySong(entry.song) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkImage(
                            artworkUri = entry.song.albumArtUri,
                            artworkKey = "${entry.song.id}_${entry.song.customArtworkUpdatedAtMs}",
                            contentDescription = entry.song.title,
                            shape = RoundedCornerShape(8.dp),
                            seedTitle = entry.song.album,
                            iconSize = 20.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.song.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NocturnePalette.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = NocturnePalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = TimeFormatUtils.formatRelativeTime(entry.playedAtMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = NocturnePalette.TextMuted
                        )
                    }
                }
            }
        }
    }
}
