package com.nocturne.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.SearchResults
import com.nocturne.music.model.Song
import com.nocturne.music.ui.components.AlbumArtworkCard
import com.nocturne.music.ui.components.ArtistArtworkCard
import com.nocturne.music.ui.components.PlaylistCard
import com.nocturne.music.ui.components.SectionHeader
import com.nocturne.music.ui.components.SongRowItem

enum class SearchFilterCategory(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    GENRES("Genres")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    searchResults: SearchResults,
    searchHistory: List<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveHistoryQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val focusManager = LocalFocusManager.current
    var selectedCategory by remember { mutableStateOf(SearchFilterCategory.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.displayMedium,
            color = NocturnePalette.TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search songs, albums, artists, genres, playlists…",
                    color = NocturnePalette.TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = atmosphere.primaryAccent
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear Search",
                            tint = NocturnePalette.TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onCommitSearch(query)
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NocturnePalette.ObsidianSurface,
                unfocusedContainerColor = NocturnePalette.ObsidianSurface,
                focusedBorderColor = atmosphere.primaryAccent,
                unfocusedBorderColor = NocturnePalette.GlassBorderHairline,
                focusedTextColor = NocturnePalette.TextPrimary,
                unfocusedTextColor = NocturnePalette.TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter category pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilterCategory.entries.forEach { category ->
                val active = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) atmosphere.primaryAccent else NocturnePalette.CharcoalSurface
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) NocturnePalette.AmoledBlack else NocturnePalette.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 152.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (query.isBlank()) {
                if (searchHistory.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent Searches",
                            actionLabel = "Clear All",
                            onActionClick = onClearHistory
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { historyQuery ->
                                Row(
                                    modifier = Modifier
                                        .matteSurface(RoundedCornerShape(50))
                                        .clickable { onQueryChange(historyQuery) }
                                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = NocturnePalette.TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = historyQuery,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NocturnePalette.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Delete",
                                        tint = NocturnePalette.TextMuted,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { onRemoveHistoryQuery(historyQuery) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type to search your offline library across titles, artists, albums, folders, and genres.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NocturnePalette.TextMuted
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matches found for \"$query\".",
                            style = MaterialTheme.typography.titleMedium,
                            color = NocturnePalette.TextSecondary
                        )
                    }
                }
            } else {
                // Songs section
                if ((selectedCategory == SearchFilterCategory.ALL || selectedCategory == SearchFilterCategory.SONGS) &&
                    searchResults.songs.isNotEmpty()
                ) {
                    item {
                        SectionHeader(
                            title = "Songs (${searchResults.songs.size})",
                            actionLabel = "Play Matches",
                            onActionClick = {
                                onCommitSearch(query)
                                onPlaySongs(searchResults.songs, 0, false)
                            }
                        )
                    }
                    itemsIndexed(
                        items = searchResults.songs,
                        key = { _, s -> "search_s_${s.id}" }
                    ) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = {
                                onCommitSearch(query)
                                onPlaySongs(searchResults.songs, idx, false)
                            },
                            onFavoriteClick = { onToggleSongFavorite(song) },
                            onMoreClick = { onOpenTrackContext(song) }
                        )
                    }
                }

                // Albums section
                if ((selectedCategory == SearchFilterCategory.ALL || selectedCategory == SearchFilterCategory.ALBUMS) &&
                    searchResults.albums.isNotEmpty()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Albums (${searchResults.albums.size})")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults.albums, key = { "search_al_${it.id}" }) { album ->
                                AlbumArtworkCard(
                                    album = album,
                                    onClick = {
                                        onCommitSearch(query)
                                        onOpenAlbum(album.id)
                                    },
                                    modifier = Modifier.width(154.dp)
                                )
                            }
                        }
                    }
                }

                // Artists section
                if ((selectedCategory == SearchFilterCategory.ALL || selectedCategory == SearchFilterCategory.ARTISTS) &&
                    searchResults.artists.isNotEmpty()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Artists (${searchResults.artists.size})")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults.artists, key = { "search_ar_${it.id}" }) { artist ->
                                ArtistArtworkCard(
                                    artist = artist,
                                    onClick = {
                                        onCommitSearch(query)
                                        onOpenArtist(artist.id)
                                    },
                                    modifier = Modifier.width(128.dp)
                                )
                            }
                        }
                    }
                }

                // Playlists section
                if ((selectedCategory == SearchFilterCategory.ALL || selectedCategory == SearchFilterCategory.PLAYLISTS) &&
                    searchResults.playlists.isNotEmpty()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Playlists (${searchResults.playlists.size})")
                    }
                    items(searchResults.playlists, key = { "search_pl_${it.id}" }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = {
                                onCommitSearch(query)
                                onOpenPlaylist(playlist.id)
                            },
                            onPlayClick = {
                                onCommitSearch(query)
                                if (playlist.songs.isNotEmpty()) {
                                    onPlaySongs(playlist.songs, 0, false)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }

                // Genres section
                if ((selectedCategory == SearchFilterCategory.ALL || selectedCategory == SearchFilterCategory.GENRES) &&
                    searchResults.genres.isNotEmpty()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Genres (${searchResults.genres.size})")
                    }
                    items(searchResults.genres, key = { "search_ge_${it.name}" }) { genre ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .matteSurface(RoundedCornerShape(14.dp))
                                .clickable {
                                    onCommitSearch(query)
                                    onPlaySongs(genre.songs, 0, false)
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = genre.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = NocturnePalette.TextPrimary
                            )
                            Text(
                                text = "${genre.songCount} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = NocturnePalette.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
