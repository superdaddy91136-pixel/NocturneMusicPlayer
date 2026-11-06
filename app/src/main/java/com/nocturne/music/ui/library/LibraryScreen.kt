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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.LocalArtworkAtmosphere
import com.nocturne.music.designsystem.NocturnePalette
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
import com.nocturne.music.ui.components.FavoriteButton
import com.nocturne.music.ui.components.PlaylistCard
import com.nocturne.music.ui.components.SectionHeader
import com.nocturne.music.ui.components.SongRowItem

sealed interface LibraryView {
    data object Main : LibraryView
    data object LikedSongs : LibraryView
    data object AllSongs : LibraryView
    data object AllAlbums : LibraryView
    data object AllArtists : LibraryView
    data object AllPlaylists : LibraryView
    data object SavedPlaylists : LibraryView
    data object Genres : LibraryView
    data object Recent : LibraryView
    data object Folders : LibraryView
}

sealed interface LibraryDrillDown {
    data class AlbumDetail(val albumId: Long) : LibraryDrillDown
    data class ArtistDetail(val artistId: Long) : LibraryDrillDown
    data class PlaylistDetail(val playlistId: Long) : LibraryDrillDown
    data class GenreDetail(val genreName: String) : LibraryDrillDown
    data class FolderDetail(val folderPath: String) : LibraryDrillDown
}

private data class LibraryCategory(
    val title: String,
    val icon: ImageVector,
    val countText: String,
    val count: Int,
    val gradientColors: List<Color>,
    val accentColor: Color,
    val onClick: () -> Unit
)

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
    pinnedAlbums: List<Album>,
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
    onToggleAlbumPinned: (Album) -> Unit,
    onToggleArtistFavorite: (Artist) -> Unit,
    onToggleGridList: (Boolean) -> Unit,
    onToggleLandingGridList: (Boolean) -> Unit,
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
    onDrillDownActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val atmosphere = LocalArtworkAtmosphere.current
    var currentView by remember { mutableStateOf<LibraryView>(LibraryView.Main) }
    val isGridView = userSettings.isLibraryGridView
    val isLandingGridView = userSettings.isLibraryLandingGridView
    var drillDownStack by remember { mutableStateOf<List<LibraryDrillDown>>(emptyList()) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(externalDrillDown) {
        if (externalDrillDown != null) {
            if (drillDownStack.lastOrNull() != externalDrillDown) {
                drillDownStack = listOf(externalDrillDown)
            }
        } else if (drillDownStack.isNotEmpty() && drillDownStack.all { it == externalDrillDown }) {
            drillDownStack = emptyList()
        }
    }

    val handleBack = {
        if (drillDownStack.isNotEmpty()) {
            val updated = drillDownStack.dropLast(1)
            drillDownStack = updated
            if (updated.isEmpty() && externalDrillDown != null) {
                onClearExternalDrillDown()
            }
        } else if (externalDrillDown != null) {
            onClearExternalDrillDown()
        } else if (currentView != LibraryView.Main) {
            currentView = LibraryView.Main
        }
    }

    androidx.activity.compose.BackHandler(
        enabled = drillDownStack.isNotEmpty() || externalDrillDown != null || currentView != LibraryView.Main
    ) {
        handleBack()
    }

    val activeDrillDown = drillDownStack.lastOrNull() ?: externalDrillDown

    LaunchedEffect(activeDrillDown != null) {
        onDrillDownActiveChanged(activeDrillDown != null)
    }

    val closeDrillDown = handleBack

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
                        onToggleAlbumPinned = { onToggleAlbumPinned(album) },
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
                        onOpenAlbum = { albumId -> drillDownStack = drillDownStack + LibraryDrillDown.AlbumDetail(albumId) },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentView != LibraryView.Main) {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = NocturnePalette.TextPrimary
                        )
                    }
                }
                Column {
                    Text(
                        text = when (currentView) {
                            LibraryView.Main -> "Library"
                            LibraryView.LikedSongs -> "Liked Songs"
                            LibraryView.AllSongs -> "All Songs"
                            LibraryView.AllAlbums -> "All Albums"
                            LibraryView.AllArtists -> "All Artists"
                            LibraryView.AllPlaylists -> "All Playlists"
                            LibraryView.SavedPlaylists -> "Saved Playlists"
                            LibraryView.Genres -> "Genres"
                            LibraryView.Recent -> "Recent"
                            LibraryView.Folders -> "Folders"
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = NocturnePalette.TextPrimary
                    )
                    if (currentView == LibraryView.Main) {
                        Text(
                            text = "${songs.size} songs • ${albums.size} albums",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocturnePalette.TextMuted
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentView == LibraryView.AllAlbums || currentView == LibraryView.AllArtists) {
                    IconButton(
                        onClick = { onToggleGridList(!isGridView) },
                        modifier = Modifier
                            .size(40.dp)
                            .matteSurface(CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle View",
                            tint = NocturnePalette.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (currentView != LibraryView.Main) {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .matteSurface(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
                                tint = NocturnePalette.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        LibrarySortDropdown(
                            expanded = showSortMenu,
                            currentView = currentView,
                            userSettings = userSettings,
                            onDismiss = { showSortMenu = false },
                            onSetSongSort = onSetSongSort,
                            onSetAlbumSort = onSetAlbumSort,
                            onSetArtistSort = onSetArtistSort
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onToggleLandingGridList(!isLandingGridView) },
                        modifier = Modifier
                            .size(40.dp)
                            .matteSurface(CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isLandingGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle Landing View",
                            tint = NocturnePalette.TextPrimary,
                            modifier = Modifier.size(20.dp)
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Content
        when (currentView) {
            LibraryView.Main -> {
                val categories = listOf(
                    LibraryCategory(
                        title = "Liked Songs",
                        icon = Icons.Rounded.Favorite,
                        countText = "${favoriteSongs.size} tracks",
                        count = favoriteSongs.size,
                        gradientColors = listOf(Color(0xFFD65C7D), Color(0xFFD9B8BC)),
                        accentColor = Color(0xFFD65C7D),
                        onClick = { currentView = LibraryView.LikedSongs }
                    ),
                    LibraryCategory(
                        title = "All Songs",
                        icon = Icons.Rounded.MusicNote,
                        countText = "${songs.size} tracks",
                        count = songs.size,
                        gradientColors = listOf(Color(0xFF286A9B), Color(0xFF72A9C5)),
                        accentColor = Color(0xFF286A9B),
                        onClick = { currentView = LibraryView.AllSongs }
                    ),
                    LibraryCategory(
                        title = "All Albums",
                        icon = Icons.Rounded.Album,
                        countText = "${albums.size} albums",
                        count = albums.size,
                        gradientColors = listOf(Color(0xFF176B55), Color(0xFF79B99A)),
                        accentColor = Color(0xFF176B55),
                        onClick = { currentView = LibraryView.AllAlbums }
                    ),
                    LibraryCategory(
                        title = "All Artists",
                        icon = Icons.Rounded.Person,
                        countText = "${artists.size} artists",
                        count = artists.size,
                        gradientColors = listOf(Color(0xFFB87932), Color(0xFFE2B66D)),
                        accentColor = Color(0xFFB87932),
                        onClick = { currentView = LibraryView.AllArtists }
                    ),
                    LibraryCategory(
                        title = "All Playlists",
                        icon = Icons.Rounded.LibraryMusic,
                        countText = "${playlists.size} playlists",
                        count = playlists.size,
                        gradientColors = listOf(Color(0xFF6947A8), Color(0xFFB18BD5)),
                        accentColor = Color(0xFF6947A8),
                        onClick = { currentView = LibraryView.AllPlaylists }
                    ),
                    LibraryCategory(
                        title = "Saved Playlists",
                        icon = Icons.Rounded.QueueMusic,
                        countText = "${playlists.size} saved",
                        count = playlists.size,
                        gradientColors = listOf(Color(0xFF167E82), Color(0xFF70C7BD)),
                        accentColor = Color(0xFF167E82),
                        onClick = { currentView = LibraryView.SavedPlaylists }
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    if (pinnedAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Pinned Albums")
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(pinnedAlbums, key = { "pinned_${it.id}" }) { album ->
                                    Box {
                                        AlbumArtworkCard(
                                            album = album,
                                            onClick = { drillDownStack = drillDownStack + LibraryDrillDown.AlbumDetail(album.id) },
                                            modifier = Modifier.width(140.dp)
                                        )
                                        IconButton(
                                            onClick = { onToggleAlbumPinned(album) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .background(
                                                    NocturnePalette.CharcoalSurface.copy(alpha = 0.8f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PushPin,
                                                contentDescription = "Unpin",
                                                tint = NocturnePalette.EmeraldGlow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    if (isLandingGridView) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                categories.chunked(2).forEach { rowCategories ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        rowCategories.forEach { category ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                LibraryCategoryCard(category = category)
                                            }
                                        }
                                        if (rowCategories.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(categories, key = { "cat_${it.title}" }) { category ->
                            LibraryCategoryRow(
                                title = category.title,
                                icon = category.icon,
                                itemCount = category.countText,
                                accentColor = category.accentColor,
                                onClick = category.onClick
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "More")

                        LibraryCategoryRow(
                            title = "Genres",
                            icon = Icons.Rounded.LibraryMusic,
                            itemCount = "${genres.size} genres",
                            accentColor = NocturnePalette.EmeraldGlow,
                            onClick = { currentView = LibraryView.Genres }
                        )
                        LibraryCategoryRow(
                            title = "Recent",
                            icon = Icons.Rounded.Refresh,
                            itemCount = "${recentlyPlayed.size} played",
                            accentColor = NocturnePalette.EmeraldGlow,
                            onClick = { currentView = LibraryView.Recent }
                        )
                        LibraryCategoryRow(
                            title = "Folders",
                            icon = Icons.Rounded.Folder,
                            itemCount = "${folders.size} folders",
                            accentColor = NocturnePalette.EmeraldGlow,
                            onClick = { currentView = LibraryView.Folders }
                        )
                    }
                }
            }

            LibraryView.LikedSongs -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ActionChipButton(
                            label = "Play All (${favoriteSongs.size})",
                            icon = Icons.Rounded.PlayArrow,
                            primary = true,
                            onClick = { if (favoriteSongs.isNotEmpty()) onPlaySongs(favoriteSongs, 0, false) },
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .fillMaxWidth()
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
                }
            }

            LibraryView.AllSongs -> {
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

            LibraryView.AllAlbums -> {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 150.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albums, key = { "lib_album_${it.id}" }) { album ->
                            AlbumArtworkCard(
                                album = album,
                                onClick = { drillDownStack = drillDownStack + LibraryDrillDown.AlbumDetail(album.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 150.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albums, key = { "lib_album_${it.id}" }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .matteSurface(RoundedCornerShape(14.dp))
                                    .clickable { drillDownStack = drillDownStack + LibraryDrillDown.AlbumDetail(album.id) }
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
                                    Text(album.title, style = MaterialTheme.typography.titleMedium, color = NocturnePalette.TextPrimary)
                                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = NocturnePalette.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            LibraryView.AllArtists -> {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 116.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 150.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artists, key = { "lib_artist_${it.id}" }) { artist ->
                            ArtistArtworkCard(
                                artist = artist,
                                onClick = { drillDownStack = drillDownStack + LibraryDrillDown.ArtistDetail(artist.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 150.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artists, key = { "lib_artist_${it.id}" }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                                    .matteSurface(RoundedCornerShape(14.dp))
                                    .clickable { drillDownStack = drillDownStack + LibraryDrillDown.ArtistDetail(artist.id) }
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
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium, color = NocturnePalette.TextPrimary)
                                    Text("${artist.songCount} tracks", style = MaterialTheme.typography.bodySmall, color = NocturnePalette.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            LibraryView.AllPlaylists, LibraryView.SavedPlaylists -> {
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
                            onClick = { drillDownStack = drillDownStack + LibraryDrillDown.PlaylistDetail(playlist.id) },
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

            LibraryView.Genres -> {
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
                                .clickable { drillDownStack = drillDownStack + LibraryDrillDown.GenreDetail(genre.name) }
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

            LibraryView.Recent -> {
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

            LibraryView.Folders -> {
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
                                    drillDownStack = drillDownStack + LibraryDrillDown.FolderDetail(folder.path)
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
private fun LibraryCategoryCard(
    category: LibraryCategory,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(colors = category.gradientColors),
                shape = cardShape
            )
            .clickable(onClick = category.onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibraryCategoryRow(
    title: String,
    icon: ImageVector,
    itemCount: String? = null,
    accentColor: Color = NocturnePalette.EmeraldGlow,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    accentColor.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NocturnePalette.TextPrimary
            )
            if (itemCount != null) {
                Text(
                    text = itemCount,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextMuted
                )
            }
        }
    }
}

@Composable
private fun LibrarySortDropdown(
    expanded: Boolean,
    currentView: LibraryView,
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
        when (currentView) {
            LibraryView.AllAlbums -> {
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

            LibraryView.AllArtists -> {
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
    onToggleAlbumPinned: () -> Unit,
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
                    FavoriteButton(
                        isFavorite = album.isFavorite,
                        onClick = onToggleAlbumFavorite,
                        iconSize = 24.dp,
                        buttonSize = 48.dp,
                        unfavoriteColor = NocturnePalette.TextSecondary
                    )
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = NocturnePalette.TextSecondary
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(NocturnePalette.CharcoalElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (album.isPinned) "Unpin Album" else "Pin Album", color = NocturnePalette.TextPrimary) },
                                onClick = { 
                                    onToggleAlbumPinned()
                                    showMenu = false 
                                }
                            )
                        }
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
                FavoriteButton(
                    isFavorite = artist.isFavorite,
                    onClick = onToggleArtistFavorite,
                    iconSize = 24.dp,
                    buttonSize = 48.dp,
                    unfavoriteColor = NocturnePalette.TextSecondary
                )
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
    icon: ImageVector,
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
