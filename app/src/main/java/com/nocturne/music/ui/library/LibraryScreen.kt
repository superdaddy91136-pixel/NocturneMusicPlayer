package com.nocturne.music.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
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
import com.nocturne.music.model.Album
import com.nocturne.music.model.AlbumSortOption
import com.nocturne.music.model.Artist
import com.nocturne.music.model.ArtistSortOption
import com.nocturne.music.model.Genre
import com.nocturne.music.model.MusicFolder
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.Song
import com.nocturne.music.model.SongSortOption
import com.nocturne.music.model.SortDirection
import com.nocturne.music.model.TimeFormatUtils
import com.nocturne.music.model.UserSettings
import com.nocturne.music.ui.components.AlbumArtworkCard
import com.nocturne.music.ui.components.ArtistArtworkCard
import com.nocturne.music.ui.components.ArtworkImage
import com.nocturne.music.ui.components.PlaylistCard
import com.nocturne.music.ui.components.SectionHeader
import com.nocturne.music.ui.components.SongRowItem

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    GENRES("Genres"),
    FAVOURITES("Favourites"),
    RECENT("Recent"),
    FOLDERS("Folders")
}

sealed interface LibraryDrillDown {
    data class AlbumDetail(val albumId: Long) : LibraryDrillDown
    data class ArtistDetail(val artistId: Long) : LibraryDrillDown
    data class PlaylistDetail(val playlistId: Long) : LibraryDrillDown
    data class GenreDetail(val genreName: String) : LibraryDrillDown
    data class FolderDetail(val folderPath: String) : LibraryDrillDown
}

@Composable
fun LibraryScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    genres: List<Genre>,
    folders: List<MusicFolder>,
    favoriteSongs: List<Song>,
    favoriteAlbums: List<Album>,
    favoriteArtists: List<Artist>,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    userSettings: UserSettings,
    externalDrillDown: LibraryDrillDown?,
    onClearExternalDrillDown: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean) -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onToggleAlbumFavorite: (Album) -> Unit,
    onToggleArtistFavorite: (Artist) -> Unit,
    onOpenTrackContext: (Song) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRenamePlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onSetSongSort: (SongSortOption, SortDirection) -> Unit,
    onSetAlbumSort: (AlbumSortOption) -> Unit,
    onSetArtistSort: (ArtistSortOption) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onIncludeFolder: (String) -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var internalDrillDown by remember { mutableStateOf<LibraryDrillDown?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val activeDrillDown = externalDrillDown ?: internalDrillDown

    val closeDrillDown = {
        internalDrillDown = null
        onClearExternalDrillDown()
    }

    if (activeDrillDown != null) {
        when (activeDrillDown) {
            is LibraryDrillDown.AlbumDetail -> {
                val album = albums.find { it.id == activeDrillDown.albumId }
                if (album != null) {
                    AlbumDetailView(
                        album = album,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBack = closeDrillDown,
                        onPlayAlbum = { shuffle -> onPlaySongs(album.songs, 0, shuffle) },
                        onPlaySongAt = { idx -> onPlaySongs(album.songs, idx, false) },
                        onToggleAlbumFavorite = { onToggleAlbumFavorite(album) },
                        onToggleSongFavorite = onToggleSongFavorite,
                        onOpenTrackContext = onOpenTrackContext
                    )
                    return
                }
            }

            is LibraryDrillDown.ArtistDetail -> {
                val artist = artists.find { it.id == activeDrillDown.artistId }
                if (artist != null) {
                    ArtistDetailView(
                        artist = artist,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBack = closeDrillDown,
                        onPlayArtist = { shuffle -> onPlaySongs(artist.songs, 0, shuffle) },
                        onOpenAlbum = { albumId -> internalDrillDown = LibraryDrillDown.AlbumDetail(albumId) },
                        onPlaySongAt = { idx -> onPlaySongs(artist.songs, idx, false) },
                        onToggleArtistFavorite = { onToggleArtistFavorite(artist) },
                        onToggleSongFavorite = onToggleSongFavorite,
                        onOpenTrackContext = onOpenTrackContext
                    )
                    return
                }
            }

            is LibraryDrillDown.PlaylistDetail -> {
                val playlist = playlists.find { it.id == activeDrillDown.playlistId }
                if (playlist != null) {
                    PlaylistDetailView(
                        playlist = playlist,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBack = closeDrillDown,
                        onPlayPlaylist = { shuffle -> onPlaySongs(playlist.songs, 0, shuffle) },
                        onPlaySongAt = { idx -> onPlaySongs(playlist.songs, idx, false) },
                        onRenamePlaylist = { onRenamePlaylistClick(playlist) },
                        onDeletePlaylist = {
                            onDeletePlaylistClick(playlist)
                            closeDrillDown()
                        },
                        onRemoveSong = { songId -> onRemoveSongFromPlaylist(playlist.id, songId) },
                        onToggleSongFavorite = onToggleSongFavorite,
                        onOpenTrackContext = onOpenTrackContext
                    )
                    return
                }
            }

            is LibraryDrillDown.GenreDetail -> {
                val genre = genres.find { it.name == activeDrillDown.genreName }
                if (genre != null) {
                    CollectionTrackListDetailView(
                        title = genre.name,
                        subtitle = "${genre.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(genre.totalDurationMs)}",
                        artworkUri = genre.artworkUri,
                        songs = genre.songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBack = closeDrillDown,
                        onPlayAll = { shuffle -> onPlaySongs(genre.songs, 0, shuffle) },
                        onPlaySongAt = { idx -> onPlaySongs(genre.songs, idx, false) },
                        onToggleSongFavorite = onToggleSongFavorite,
                        onOpenTrackContext = onOpenTrackContext
                    )
                    return
                }
            }

            is LibraryDrillDown.FolderDetail -> {
                val folder = folders.find { it.path == activeDrillDown.folderPath }
                if (folder != null) {
                    CollectionTrackListDetailView(
                        title = folder.name,
                        subtitle = folder.path,
                        artworkUri = folder.songs.firstNotNullOfOrNull { it.albumArtUri },
                        songs = folder.songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onBack = closeDrillDown,
                        onPlayAll = { shuffle -> onPlaySongs(folder.songs, 0, shuffle) },
                        onPlaySongAt = { idx -> onPlaySongs(folder.songs, idx, false) },
                        onToggleSongFavorite = onToggleSongFavorite,
                        onOpenTrackContext = onOpenTrackContext
                    )
                    return
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Header & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.displayMedium,
                    color = NocturnePalette.TextPrimary
                )
                Text(
                    text = "${songs.size} songs • ${albums.size} albums • ${artists.size} artists",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .matteSurface(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort Library",
                            tint = NocturnePalette.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    LibrarySortDropdown(
                        expanded = showSortMenu,
                        selectedTab = selectedTab,
                        userSettings = userSettings,
                        onDismiss = { showSortMenu = false },
                        onSetSongSort = onSetSongSort,
                        onSetAlbumSort = onSetAlbumSort,
                        onSetArtistSort = onSetArtistSort
                    )
                }

                IconButton(
                    onClick = onRescanLibrary,
                    modifier = Modifier
                        .size(40.dp)
                        .matteSurface(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Rescan",
                        tint = NocturnePalette.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .matteSurface(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = NocturnePalette.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Scrollable Liquid-Glass Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) atmosphere.primaryAccent else NocturnePalette.CharcoalSurface
                        )
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) NocturnePalette.AmoledBlack else NocturnePalette.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tab Content
        when (selectedTab) {
            LibraryTab.SONGS -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ActionChipButton(
                                label = "Play All (${songs.size})",
                                icon = Icons.Rounded.PlayArrow,
                                primary = true,
                                onClick = { if (songs.isNotEmpty()) onPlaySongs(songs, 0, false) },
                                modifier = Modifier.weight(1f)
                            )
                            ActionChipButton(
                                label = "Shuffle",
                                icon = Icons.Rounded.Shuffle,
                                primary = false,
                                onClick = { if (songs.isNotEmpty()) onPlaySongs(songs, 0, true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    itemsIndexed(songs, key = { _, s -> "lib_song_${s.id}" }) { index, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySongs(songs, index, false) },
                            onFavoriteClick = { onToggleSongFavorite(song) },
                            onMoreClick = { onOpenTrackContext(song) }
                        )
                    }
                }
            }

            LibraryTab.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albums, key = { "lib_album_${it.id}" }) { album ->
                        AlbumArtworkCard(
                            album = album,
                            onClick = { internalDrillDown = LibraryDrillDown.AlbumDetail(album.id) }
                        )
                    }
                }
            }

            LibraryTab.ARTISTS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 116.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(artists, key = { "lib_artist_${it.id}" }) { artist ->
                        ArtistArtworkCard(
                            artist = artist,
                            onClick = { internalDrillDown = LibraryDrillDown.ArtistDetail(artist.id) }
                        )
                    }
                }
            }

            LibraryTab.PLAYLISTS -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ActionChipButton(
                            label = "Create New Playlist",
                            icon = Icons.Rounded.Add,
                            primary = true,
                            onClick = onCreatePlaylistClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(playlists, key = { "lib_pl_${it.id}" }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { internalDrillDown = LibraryDrillDown.PlaylistDetail(playlist.id) },
                            onPlayClick = {
                                if (playlist.songs.isNotEmpty()) {
                                    onPlaySongs(playlist.songs, 0, false)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(150.dp)) }
                }
            }

            LibraryTab.GENRES -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(genres, key = { "lib_genre_${it.name}" }) { genre ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matteSurface(RoundedCornerShape(16.dp))
                                .clickable { internalDrillDown = LibraryDrillDown.GenreDetail(genre.name) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artworkUri = genre.artworkUri,
                                contentDescription = genre.name,
                                shape = RoundedCornerShape(10.dp),
                                seedTitle = genre.name,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NocturnePalette.TextPrimary
                                )
                                Text(
                                    text = "${genre.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(genre.totalDurationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NocturnePalette.TextSecondary
                                )
                            }
                            IconButton(onClick = { onPlaySongs(genre.songs, 0, false) }) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Genre",
                                    tint = NocturnePalette.TextPrimary
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(150.dp)) }
                }
            }

            LibraryTab.FAVOURITES -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SectionHeader(
                            title = "Favourite Songs (${favoriteSongs.size})",
                            actionLabel = if (favoriteSongs.isNotEmpty()) "Play All" else null,
                            onActionClick = { onPlaySongs(favoriteSongs, 0, false) }
                        )
                    }
                    itemsIndexed(favoriteSongs, key = { _, s -> "fav_s_${s.id}" }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySongs(favoriteSongs, idx, false) },
                            onFavoriteClick = { onToggleSongFavorite(song) },
                            onMoreClick = { onOpenTrackContext(song) }
                        )
                    }

                    if (favoriteAlbums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader(title = "Favourite Albums (${favoriteAlbums.size})")
                        }
                        items(favoriteAlbums, key = { "fav_al_${it.id}" }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .matteSurface(RoundedCornerShape(14.dp))
                                    .clickable { internalDrillDown = LibraryDrillDown.AlbumDetail(album.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkImage(
                                    artworkUri = album.albumArtUri,
                                    contentDescription = album.title,
                                    shape = RoundedCornerShape(10.dp),
                                    seedTitle = album.title,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album.title, style = MaterialTheme.typography.titleMedium)
                                    Text(album.artist, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (favoriteArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader(title = "Favourite Artists (${favoriteArtists.size})")
                        }
                        items(favoriteArtists, key = { "fav_ar_${it.id}" }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .matteSurface(RoundedCornerShape(14.dp))
                                    .clickable { internalDrillDown = LibraryDrillDown.ArtistDetail(artist.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkImage(
                                    artworkUri = artist.artworkUri,
                                    contentDescription = artist.name,
                                    shape = CircleShape,
                                    seedTitle = artist.name,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${artist.songCount} tracks", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            LibraryTab.RECENT -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SectionHeader(
                            title = "Recently Played (${recentlyPlayed.size})",
                            actionLabel = if (recentlyPlayed.isNotEmpty()) "Play" else null,
                            onActionClick = { onPlaySongs(recentlyPlayed, 0, false) }
                        )
                    }
                    itemsIndexed(recentlyPlayed.take(20), key = { _, s -> "rec_p_${s.id}" }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySongs(recentlyPlayed, idx, false) },
                            onFavoriteClick = { onToggleSongFavorite(song) },
                            onMoreClick = { onOpenTrackContext(song) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(
                            title = "Recently Added (${recentlyAdded.size})",
                            actionLabel = if (recentlyAdded.isNotEmpty()) "Play" else null,
                            onActionClick = { onPlaySongs(recentlyAdded, 0, false) }
                        )
                    }
                    itemsIndexed(recentlyAdded.take(20), key = { _, s -> "rec_a_${s.id}" }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySongs(recentlyAdded, idx, false) },
                            onFavoriteClick = { onToggleSongFavorite(song) },
                            onMoreClick = { onOpenTrackContext(song) }
                        )
                    }
                }
            }

            LibraryTab.FOLDERS -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(folders, key = { "folder_${it.path}" }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .matteSurface(RoundedCornerShape(16.dp))
                                .clickable(enabled = !folder.isExcluded) {
                                    internalDrillDown = LibraryDrillDown.FolderDetail(folder.path)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (folder.isExcluded) Icons.Rounded.FolderOff else Icons.Rounded.Folder,
                                contentDescription = folder.name,
                                tint = if (folder.isExcluded) NocturnePalette.CrimsonRose else atmosphere.primaryAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NocturnePalette.TextPrimary
                                )
                                Text(
                                    text = folder.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NocturnePalette.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (folder.isExcluded) {
                                        "Excluded from library scan"
                                    } else {
                                        "${folder.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(folder.totalDurationMs)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (folder.isExcluded) NocturnePalette.CrimsonRose else NocturnePalette.TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NocturnePalette.CharcoalElevated)
                                    .clickable {
                                        if (folder.isExcluded) onIncludeFolder(folder.path)
                                        else onExcludeFolder(folder.path)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (folder.isExcluded) "Restore" else "Exclude",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (folder.isExcluded) atmosphere.primaryAccent else NocturnePalette.TextSecondary
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(150.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LibrarySortDropdown(
    expanded: Boolean,
    selectedTab: LibraryTab,
    userSettings: UserSettings,
    onDismiss: () -> Unit,
    onSetSongSort: (SongSortOption, SortDirection) -> Unit,
    onSetAlbumSort: (AlbumSortOption) -> Unit,
    onSetArtistSort: (ArtistSortOption) -> Unit
) {
    val atmosphere = LocalArtworkAtmosphere.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(NocturnePalette.CharcoalSurface)
    ) {
        when (selectedTab) {
            LibraryTab.ALBUMS -> {
                AlbumSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (userSettings.albumSortOption == option) {
                                    atmosphere.primaryAccent
                                } else NocturnePalette.TextPrimary
                            )
                        },
                        onClick = {
                            onSetAlbumSort(option)
                            onDismiss()
                        }
                    )
                }
            }

            LibraryTab.ARTISTS -> {
                ArtistSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (userSettings.artistSortOption == option) {
                                    atmosphere.primaryAccent
                                } else NocturnePalette.TextPrimary
                            )
                        },
                        onClick = {
                            onSetArtistSort(option)
                            onDismiss()
                        }
                    )
                }
            }

            else -> {
                SongSortOption.entries.forEach { option ->
                    val isSelected = userSettings.songSortOption == option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (isSelected) atmosphere.primaryAccent else NocturnePalette.TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = if (userSettings.songSortDirection == SortDirection.ASCENDING) {
                                            Icons.Rounded.ArrowUpward
                                        } else {
                                            Icons.Rounded.ArrowDownward
                                        },
                                        contentDescription = null,
                                        tint = atmosphere.primaryAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            val nextDir = if (isSelected && userSettings.songSortDirection == SortDirection.ASCENDING) {
                                SortDirection.DESCENDING
                            } else {
                                SortDirection.ASCENDING
                            }
                            onSetSongSort(option, nextDir)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailView(
    album: Album,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAlbum: (Boolean) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onToggleAlbumFavorite: () -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NocturnePalette.TextPrimary
                    )
                }
                IconButton(onClick = onToggleAlbumFavorite) {
                    Icon(
                        imageVector = if (album.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite Album",
                        tint = if (album.isFavorite) NocturnePalette.CrimsonRose else NocturnePalette.TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtworkImage(
                    artworkUri = album.albumArtUri,
                    contentDescription = album.title,
                    shape = RoundedCornerShape(22.dp),
                    seedTitle = album.title,
                    iconSize = 64.dp,
                    modifier = Modifier.size(210.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = NocturnePalette.TextPrimary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalArtworkAtmosphere.current.primaryAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        if (album.year > 0) append("${album.year} • ")
                        append("${album.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(album.totalDurationMs)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionChipButton(
                        label = "Play Album",
                        icon = Icons.Rounded.PlayArrow,
                        primary = true,
                        onClick = { onPlayAlbum(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChipButton(
                        label = "Shuffle",
                        icon = Icons.Rounded.Shuffle,
                        primary = false,
                        onClick = { onPlayAlbum(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        itemsIndexed(album.songs, key = { _, s -> "album_track_${s.id}" }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrentSong = currentSong?.id == song.id,
                isPlaying = isPlaying,
                trackIndexNumber = song.trackNumber.takeIf { it > 0 } ?: (idx + 1),
                onClick = { onPlaySongAt(idx) },
                onFavoriteClick = { onToggleSongFavorite(song) },
                onMoreClick = { onOpenTrackContext(song) }
            )
        }
    }
}

@Composable
private fun ArtistDetailView(
    artist: Artist,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayArtist: (Boolean) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onToggleArtistFavorite: () -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NocturnePalette.TextPrimary
                    )
                }
                IconButton(onClick = onToggleArtistFavorite) {
                    Icon(
                        imageVector = if (artist.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite Artist",
                        tint = if (artist.isFavorite) NocturnePalette.CrimsonRose else NocturnePalette.TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtworkImage(
                    artworkUri = artist.artworkUri,
                    contentDescription = artist.name,
                    shape = CircleShape,
                    seedTitle = artist.name,
                    iconSize = 56.dp,
                    modifier = Modifier.size(160.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = NocturnePalette.TextPrimary
                )
                Text(
                    text = "${artist.albumCount} albums • ${artist.songCount} songs • ${TimeFormatUtils.formatHumanReadableDuration(artist.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionChipButton(
                        label = "Play All",
                        icon = Icons.Rounded.PlayArrow,
                        primary = true,
                        onClick = { onPlayArtist(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChipButton(
                        label = "Shuffle Artist",
                        icon = Icons.Rounded.Shuffle,
                        primary = false,
                        onClick = { onPlayArtist(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (artist.albums.isNotEmpty()) {
            item {
                SectionHeader(title = "Albums (${artist.albums.size})")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    artist.albums.forEach { album ->
                        AlbumArtworkCard(
                            album = album,
                            onClick = { onOpenAlbum(album.id) },
                            modifier = Modifier.width(152.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            SectionHeader(title = "Tracks (${artist.songs.size})")
        }

        itemsIndexed(artist.songs, key = { _, s -> "artist_song_${s.id}" }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrentSong = currentSong?.id == song.id,
                isPlaying = isPlaying,
                onClick = { onPlaySongAt(idx) },
                onFavoriteClick = { onToggleSongFavorite(song) },
                onMoreClick = { onOpenTrackContext(song) }
            )
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayPlaylist: (Boolean) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveSong: (Long) -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NocturnePalette.TextPrimary
                    )
                }
                Row {
                    IconButton(onClick = onRenamePlaylist) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Playlist",
                            tint = NocturnePalette.TextSecondary
                        )
                    }
                    IconButton(onClick = onDeletePlaylist) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Playlist",
                            tint = NocturnePalette.CrimsonRose
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtworkImage(
                    artworkUri = playlist.coverArtUri,
                    contentDescription = playlist.name,
                    shape = RoundedCornerShape(20.dp),
                    seedTitle = playlist.name,
                    iconSize = 54.dp,
                    modifier = Modifier.size(180.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = NocturnePalette.TextPrimary
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NocturnePalette.TextSecondary
                    )
                }
                Text(
                    text = "${playlist.songCount} tracks • ${TimeFormatUtils.formatHumanReadableDuration(playlist.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NocturnePalette.TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionChipButton(
                        label = "Play Playlist",
                        icon = Icons.Rounded.PlayArrow,
                        primary = true,
                        onClick = { if (playlist.songs.isNotEmpty()) onPlayPlaylist(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionChipButton(
                        label = "Shuffle",
                        icon = Icons.Rounded.Shuffle,
                        primary = false,
                        onClick = { if (playlist.songs.isNotEmpty()) onPlayPlaylist(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        itemsIndexed(playlist.songs, key = { idx, s -> "pl_song_${s.id}_$idx" }) { idx, song ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SongRowItem(
                        song = song,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        onClick = { onPlaySongAt(idx) },
                        onFavoriteClick = { onToggleSongFavorite(song) },
                        onMoreClick = { onOpenTrackContext(song) }
                    )
                }
                IconButton(
                    onClick = { onRemoveSong(song.id) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove from Playlist",
                        tint = NocturnePalette.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionTrackListDetailView(
    title: String,
    subtitle: String,
    artworkUri: String?,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onToggleSongFavorite: (Song) -> Unit,
    onOpenTrackContext: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = NocturnePalette.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = NocturnePalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturnePalette.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionChipButton(
                    label = "Play (${songs.size})",
                    icon = Icons.Rounded.PlayArrow,
                    primary = true,
                    onClick = { onPlayAll(false) },
                    modifier = Modifier.weight(1f)
                )
                ActionChipButton(
                    label = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    primary = false,
                    onClick = { onPlayAll(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        itemsIndexed(songs, key = { _, s -> "col_song_${s.id}" }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrentSong = currentSong?.id == song.id,
                isPlaying = isPlaying,
                onClick = { onPlaySongAt(idx) },
                onFavoriteClick = { onToggleSongFavorite(song) },
                onMoreClick = { onOpenTrackContext(song) }
            )
        }
    }
}

@Composable
private fun ActionChipButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (primary) atmosphere.primaryAccent else NocturnePalette.CharcoalElevated
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (primary) NocturnePalette.AmoledBlack else NocturnePalette.TextPrimary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (primary) NocturnePalette.AmoledBlack else NocturnePalette.TextPrimary
            )
        }
    }
}
