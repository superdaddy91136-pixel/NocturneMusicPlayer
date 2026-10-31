package com.nocturne.music.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.liquidGlassPanel
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.Artist
import com.nocturne.music.model.Genre
import com.nocturne.music.model.Song
import com.nocturne.music.ui.components.ArtistArtworkCard
import com.nocturne.music.ui.components.ArtworkImage
import com.nocturne.music.ui.components.SectionHeader

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favoriteSongs: List<Song>,
    favoriteArtists: List<Artist>,
    artists: List<Artist>,
    genres: List<Genre>,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current

    val madeForYouSongs = remember(mostPlayed, allSongs) {
        val topPlayedArtists = mostPlayed.take(5).map { it.artistId }.toSet()
        val topPlayedGenres = mostPlayed.take(5).map { it.genre }.toSet()
        val similar = allSongs.filter { it.artistId in topPlayedArtists || it.genre in topPlayedGenres }
        (mostPlayed + similar).distinctBy { it.id }.take(15)
    }

    val validGenres = remember(genres) {
        genres.filter { it.songCount >= 4 }
    }

    val topArtists = remember(artists, mostPlayed) {
        artists.sortedByDescending { artist ->
            mostPlayed.count { it.artistId == artist.id } * 10 + artist.songCount
        }.take(15)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // 1. Top Header Bar with Profile Menu (top-left) & Rescan (top-right)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar with Dropdown Menu
                Box {
                    var profileMenuExpanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(atmosphere.primaryAccent.copy(alpha = 0.2f))
                            .clickable { profileMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Profile Menu",
                            tint = atmosphere.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recents") },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenRecents()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }

                // Rescan Button
                IconButton(
                    onClick = onRescanLibrary,
                    modifier = Modifier
                        .size(42.dp)
                        .matteSurface(CircleShape)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = atmosphere.primaryAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Rescan Local Storage",
                            tint = NocturnePalette.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Empty library state
        if (allSongs.isEmpty()) {
            item {
                EmptyLibraryWelcomeCard(
                    isScanning = isScanning,
                    onRescanClick = onRescanLibrary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        }

        // 2. Recently Played Grid (4 rows x 2 columns = 8 cards) - Header text removed completely
        if (recentlyPlayed.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            val quickPicks = recentlyPlayed.take(8)
            val rows = quickPicks.chunked(2)
            items(rows.size) { rowIndex ->
                val rowSongs = rows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSongs.forEach { song ->
                        QuickPickCard(
                            song = song,
                            onClick = {
                                val globalIdx = recentlyPlayed.indexOf(song)
                                onPlaySongs(recentlyPlayed, globalIdx.coerceAtLeast(0), false)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSongs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(18.dp)) }
        }

        // 3. "Made for You" (Horizontally scrolling square cards)
        if (madeForYouSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Made for You")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(madeForYouSongs, key = { _, s -> "mfy_${s.id}" }) { idx, song ->
                        TrackArtworkSquareCard(
                            song = song,
                            onClick = { onPlaySongs(madeForYouSongs, idx, false) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 4. "Most Played" (Horizontally scrolling square cards)
        if (mostPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Most Played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(mostPlayed.take(15), key = { _, s -> "most_${s.id}" }) { idx, song ->
                        TrackArtworkSquareCard(
                            song = song,
                            onClick = { onPlaySongs(mostPlayed, idx, false) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 5. "Recently Added" (Horizontally scrolling square cards)
        if (recentlyAdded.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Added")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(recentlyAdded.take(15), key = { _, s -> "added_${s.id}" }) { idx, song ->
                        TrackArtworkSquareCard(
                            song = song,
                            onClick = { onPlaySongs(recentlyAdded, idx, false) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 6. Genre Sections (one row per genre, >= 4 songs)
        validGenres.forEach { genre ->
            item(key = "genre_sec_${genre.name}") {
                SectionHeader(title = genre.name)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(genre.songs, key = { _, s -> "genre_${genre.name}_${s.id}" }) { idx, song ->
                        TrackArtworkSquareCard(
                            song = song,
                            onClick = { onPlaySongs(genre.songs, idx, false) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // 7. "Artists You Love"
        if (topArtists.isNotEmpty()) {
            item {
                SectionHeader(title = "Artists You Love")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topArtists, key = { "top_artist_${it.id}" }) { artist ->
                        ArtistArtworkCard(
                            artist = artist,
                            onClick = { onOpenArtist(artist.id) },
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .matteSurface(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            artworkUri = song.albumArtUri,
            artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
            contentDescription = song.title,
            shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
            seedTitle = song.album,
            iconSize = 24.dp,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = NocturnePalette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
    }
}

@Composable
private fun TrackArtworkSquareCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(142.dp)) {
            ArtworkImage(
                artworkUri = song.albumArtUri,
                artworkKey = "${song.id}_${song.customArtworkUpdatedAtMs}",
                contentDescription = song.title,
                shape = RoundedCornerShape(14.dp),
                seedTitle = song.album,
                iconSize = 36.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall.copy(color = NocturnePalette.TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePalette.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyLibraryWelcomeCard(
    isScanning: Boolean,
    onRescanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassPanel(
                shape = RoundedCornerShape(22.dp),
                tintColor = atmosphere.primaryAccent,
                fillAlpha = 0.12f,
                borderAlpha = 0.26f
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isScanning) "Scanning Device Audio Storage…" else "Your Offline Library Is Empty",
            style = MaterialTheme.typography.headlineMedium,
            color = NocturnePalette.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Place MP3, FLAC, WAV, OGG, or M4A audio files in your device's Music folder and tap Scan Library below.",
            style = MaterialTheme.typography.bodyMedium,
            color = NocturnePalette.TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(atmosphere.primaryAccent)
                .clickable(enabled = !isScanning, onClick = onRescanClick)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isScanning) "Scanning…" else "Scan Device Music Now",
                style = MaterialTheme.typography.labelLarge,
                color = NocturnePalette.AmoledBlack
            )
        }
    }
}
