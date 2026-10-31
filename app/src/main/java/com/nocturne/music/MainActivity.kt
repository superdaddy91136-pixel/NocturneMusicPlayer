package com.nocturne.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.NocturneTheme
import com.nocturne.music.designsystem.artworkAmbientCanvas
import com.nocturne.music.designsystem.liquidGlassPanel
import com.nocturne.music.model.Playlist
import com.nocturne.music.model.Song
import com.nocturne.music.ui.components.AddToPlaylistDialog
import com.nocturne.music.ui.components.CreateOrRenamePlaylistDialog
import com.nocturne.music.ui.components.EqualizerBottomSheet
import com.nocturne.music.ui.components.ManualLyricsEditorDialog
import com.nocturne.music.ui.components.MetadataEditorDialog
import com.nocturne.music.ui.components.NocturneBottomNavigationBar
import com.nocturne.music.ui.components.PersistentMiniPlayer
import com.nocturne.music.ui.components.PlaybackSpeedBottomSheet
import com.nocturne.music.ui.components.PrimaryDestination
import com.nocturne.music.ui.components.SleepTimerBottomSheet
import com.nocturne.music.ui.components.TrackContextBottomSheet
import com.nocturne.music.ui.components.EditArtworkBottomSheet
import com.nocturne.music.ui.home.HomeScreen
import com.nocturne.music.ui.home.RecentsScreen
import com.nocturne.music.ui.library.LibraryDrillDown
import com.nocturne.music.ui.library.LibraryScreen
import com.nocturne.music.ui.nowplaying.NowPlayingScreen
import com.nocturne.music.ui.search.SearchScreen
import com.nocturne.music.ui.settings.SettingsScreen
import com.nocturne.music.ui.viewmodel.NocturneViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: NocturneViewModel = hiltViewModel()
            val atmosphere by viewModel.artworkAtmosphere.collectAsStateWithLifecycle()

            NocturneTheme(atmosphere = atmosphere) {
                NocturneRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NocturneRootApp(
    viewModel: NocturneViewModel
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val atmosphere by viewModel.artworkAtmosphere.collectAsStateWithLifecycle()

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val favoriteAlbums by viewModel.favoriteAlbums.collectAsStateWithLifecycle()
    val favoriteArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val listeningHistory by viewModel.listeningHistory.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val cachedLyricsCount by viewModel.cachedLyricsCount.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(PrimaryDestination.HOME) }
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isRecentsOpen by rememberSaveable { mutableStateOf(false) }
    var externalLibraryDrillDown by remember { mutableStateOf<LibraryDrillDown?>(null) }

    // Interactive Dialogs & Sheets state
    var contextMenuSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var metadataEditSong by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showPlaybackSpeedSheet by remember { mutableStateOf(false) }
    var showManualLyricsDialog by remember { mutableStateOf(false) }
    var editArtworkSong by remember { mutableStateOf<Song?>(null) }

    // Storage Access Framework LRC document picker
    val lrcPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLrcFile(uri)
        }
    }

    // Audio & Notification Permissions
    val requiredAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredAudioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[requiredAudioPermission] == true ||
            ContextCompat.checkSelfPermission(context, requiredAudioPermission) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = granted
        if (granted) {
            viewModel.scanMediaLibrary()
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.scanMediaLibrary()
        } else {
            val permsToRequest = buildList {
                add(requiredAudioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permsToRequest)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.postStatusMessage(null)
        }
    }

    BackHandler(enabled = isNowPlayingExpanded || isSettingsOpen || isRecentsOpen || externalLibraryDrillDown != null) {
        when {
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            isSettingsOpen -> isSettingsOpen = false
            isRecentsOpen -> isRecentsOpen = false
            externalLibraryDrillDown != null -> externalLibraryDrillDown = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .artworkAmbientCanvas(
                atmosphere = atmosphere,
                style = userSettings.dynamicBackgroundStyle,
                intensity = 0.55f
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isNowPlayingExpanded) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PersistentMiniPlayer(
                            playbackState = playbackState,
                            onExpandNowPlaying = { isNowPlayingExpanded = true },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipToNext,
                            onSkipPrevious = viewModel::skipToPrevious,
                            onToggleFavorite = {
                                playbackState.currentSong?.let { viewModel.toggleSongFavorite(it) }
                            }
                        )
                        NocturneBottomNavigationBar(
                            currentRoute = currentDestination.route,
                            onNavigate = { dest ->
                                isSettingsOpen = false
                                isRecentsOpen = false
                                externalLibraryDrillDown = null
                                currentDestination = dest
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding() * 0f)
            ) {
                if (!hasAudioPermission) {
                    PermissionRationaleOverlay(
                        onGrantClick = {
                            permissionLauncher.launch(arrayOf(requiredAudioPermission))
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 90.dp)
                    )
                }

                when {
                    isSettingsOpen -> {
                        SettingsScreen(
                            userSettings = userSettings,
                            equalizerState = equalizerState,
                            excludedFolders = excludedFolders,
                            cachedLyricsCount = cachedLyricsCount,
                            totalSongsCount = songs.size,
                            onBack = { isSettingsOpen = false },
                            onOpenEqualizerSheet = { showEqualizerSheet = true },
                            onSetGaplessPlayback = viewModel::setGaplessPlayback,
                            onSetCrossfadeSec = viewModel::setCrossfadeDurationSec,
                            onSetVolumeNormalization = viewModel::setVolumeNormalization,
                            onSetPreampDb = viewModel::setNormalizationPreampDb,
                            onSetRememberPosition = viewModel::setRememberPlaybackPosition,
                            onSetResumeOnBluetooth = viewModel::setResumeOnBluetooth,
                            onSetPauseOnZeroVolume = viewModel::setPauseOnZeroVolume,
                            onSetWordLevelKaraoke = viewModel::setWordLevelKaraoke,
                            onSetPreferEmbeddedLyrics = viewModel::setPreferEmbeddedLyrics,
                            onSetAutoFetchOnlineLyrics = viewModel::setAutoFetchOnlineLyrics,
                            onSetKeepScreenOnLyrics = viewModel::setKeepScreenOnLyrics,
                            onSetLyricsFontSize = viewModel::setLyricsFontSize,
                            onSetDynamicBackgroundStyle = viewModel::setDynamicBackgroundStyle,
                            onSetHighQualityArtwork = viewModel::setHighQualityArtwork,
                            onSetSmoothArtworkTransitions = viewModel::setSmoothArtworkTransitions,
                            onSetReducedMotion = viewModel::setReducedMotion,
                            onSetMinTrackDurationSec = viewModel::setMinimumTrackDurationSec,
                            onSetIgnoreRingtones = viewModel::setIgnoreRingtoneFolders,
                            onRestoreExcludedFolder = viewModel::includeFolder,
                            onRescanLibrary = viewModel::scanMediaLibrary,
                            onClearLyricsCache = viewModel::clearLyricsCache,
                            onClearArtworkCache = viewModel::clearArtworkCache,
                            onClearPlayHistory = viewModel::clearPlayHistory
                        )
                    }

                    isRecentsOpen -> {
                        RecentsScreen(
                            listeningHistory = listeningHistory,
                            onPlaySong = { song -> viewModel.playSongs(listOf(song), 0, false) },
                            onBack = { isRecentsOpen = false }
                        )
                    }

                    currentDestination == PrimaryDestination.HOME -> {
                        HomeScreen(
                            allSongs = songs,
                            recentlyPlayed = recentlyPlayed,
                            recentlyAdded = recentlyAdded,
                            mostPlayed = mostPlayed,
                            favoriteSongs = favoriteSongs,
                            favoriteArtists = favoriteArtists,
                            artists = artists,
                            genres = genres,
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            isScanning = isScanning,
                            onPlaySongs = viewModel::playSongs,
                            onOpenArtist = { artistId ->
                                externalLibraryDrillDown = LibraryDrillDown.ArtistDetail(artistId)
                                currentDestination = PrimaryDestination.LIBRARY
                            },
                            onToggleSongFavorite = viewModel::toggleSongFavorite,
                            onOpenTrackContext = { contextMenuSong = it },
                            onRescanLibrary = viewModel::scanMediaLibrary,
                            onOpenRecents = { isRecentsOpen = true },
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    }

                    currentDestination == PrimaryDestination.LIBRARY -> {
                        LibraryScreen(
                            songs = songs,
                            albums = albums,
                            artists = artists,
                            playlists = playlists,
                            genres = genres,
                            folders = folders,
                            favoriteSongs = favoriteSongs,
                            favoriteAlbums = favoriteAlbums,
                            favoriteArtists = favoriteArtists,
                            recentlyPlayed = recentlyPlayed,
                            recentlyAdded = recentlyAdded,
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            userSettings = userSettings,
                            externalDrillDown = externalLibraryDrillDown,
                            onClearExternalDrillDown = { externalLibraryDrillDown = null },
                            onPlaySongs = viewModel::playSongs,
                            onToggleSongFavorite = viewModel::toggleSongFavorite,
                            onToggleAlbumFavorite = viewModel::toggleAlbumFavorite,
                            onToggleArtistFavorite = viewModel::toggleArtistFavorite,
                            onOpenTrackContext = { contextMenuSong = it },
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onRenamePlaylistClick = { playlistToRename = it },
                            onDeletePlaylistClick = viewModel::deletePlaylist,
                            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                            onSetSongSort = viewModel::setSongSort,
                            onSetAlbumSort = viewModel::setAlbumSort,
                            onSetArtistSort = viewModel::setArtistSort,
                            onExcludeFolder = viewModel::excludeFolder,
                            onIncludeFolder = viewModel::includeFolder,
                            onRescanLibrary = viewModel::scanMediaLibrary,
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    }

                    currentDestination == PrimaryDestination.SEARCH -> {
                        SearchScreen(
                            query = searchQuery,
                            searchResults = searchResults,
                            searchHistory = searchHistory,
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            onQueryChange = viewModel::updateSearchQuery,
                            onCommitSearch = viewModel::commitSearchQueryToHistory,
                            onRemoveHistoryQuery = viewModel::removeSearchHistoryItem,
                            onClearHistory = viewModel::clearSearchHistory,
                            onPlaySongs = viewModel::playSongs,
                            onOpenAlbum = { albumId ->
                                externalLibraryDrillDown = LibraryDrillDown.AlbumDetail(albumId)
                                currentDestination = PrimaryDestination.LIBRARY
                            },
                            onOpenArtist = { artistId ->
                                externalLibraryDrillDown = LibraryDrillDown.ArtistDetail(artistId)
                                currentDestination = PrimaryDestination.LIBRARY
                            },
                            onOpenPlaylist = { playlistId ->
                                externalLibraryDrillDown = LibraryDrillDown.PlaylistDetail(playlistId)
                                currentDestination = PrimaryDestination.LIBRARY
                            },
                            onToggleSongFavorite = viewModel::toggleSongFavorite,
                            onOpenTrackContext = { contextMenuSong = it }
                        )
                    }
                }
            }
        }

        // Full-Screen Centerpiece Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && playbackState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(340)) + fadeIn(tween(260)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(220))
        ) {
            NowPlayingScreen(
                playbackState = playbackState,
                lyricsDocument = currentLyrics,
                isLoadingLyrics = isLoadingLyrics,
                userSettings = userSettings,
                onCollapse = { isNowPlayingExpanded = false },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSkipNext = viewModel::skipToNext,
                onSkipPrevious = viewModel::skipToPrevious,
                onSetPlaybackMode = viewModel::setPlaybackMode,
                onPostStatusMessage = viewModel::postStatusMessage,
                onToggleFavorite = viewModel::toggleSongFavorite,
                onSelectQueueIndex = viewModel::selectQueueItem,
                onMoveQueueItem = viewModel::moveQueueItem,
                onRemoveQueueItem = viewModel::removeQueueItem,
                onOpenTrackContext = { contextMenuSong = it },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                onOpenEqualizer = { showEqualizerSheet = true },
                onOpenPlaybackSpeed = { showPlaybackSpeedSheet = true },
                onEditArtworkClick = { editArtworkSong = playbackState.currentSong },
                onImportLrcClick = {
                    lrcPickerLauncher.launch(arrayOf("*/*"))
                },
                onEditLyricsClick = { showManualLyricsDialog = true },
                onFetchOnlineLyricsClick = viewModel::fetchOnlineLyricsForCurrentSong,
                onAdjustLyricsOffsetMs = viewModel::adjustLyricsOffset,
                onNavigateToAlbum = { albumId ->
                    isNowPlayingExpanded = false
                    externalLibraryDrillDown = LibraryDrillDown.AlbumDetail(albumId)
                    currentDestination = PrimaryDestination.LIBRARY
                },
                onNavigateToArtist = { artistId ->
                    isNowPlayingExpanded = false
                    externalLibraryDrillDown = LibraryDrillDown.ArtistDetail(artistId)
                    currentDestination = PrimaryDestination.LIBRARY
                }
            )
        }

        // Context Menu & Interactive Dialogs
        contextMenuSong?.let { targetSong ->
            TrackContextBottomSheet(
                song = targetSong,
                onDismiss = { contextMenuSong = null },
                onPlayNow = { viewModel.playSong(targetSong) },
                onPlayNext = { viewModel.playNext(targetSong) },
                onAddToQueue = { viewModel.addToQueue(targetSong) },
                onToggleFavorite = { viewModel.toggleSongFavorite(targetSong) },
                onAddToPlaylist = { addToPlaylistSong = targetSong },
                onGoToAlbum = {
                    isNowPlayingExpanded = false
                    externalLibraryDrillDown = LibraryDrillDown.AlbumDetail(targetSong.albumId)
                    currentDestination = PrimaryDestination.LIBRARY
                },
                onGoToArtist = {
                    isNowPlayingExpanded = false
                    externalLibraryDrillDown = LibraryDrillDown.ArtistDetail(targetSong.artistId)
                    currentDestination = PrimaryDestination.LIBRARY
                },
                onEditMetadata = { metadataEditSong = targetSong },
                onEditArtwork = { editArtworkSong = targetSong }
            )
        }

        addToPlaylistSong?.let { targetSong ->
            AddToPlaylistDialog(
                song = targetSong,
                playlists = playlists,
                onDismiss = { addToPlaylistSong = null },
                onSelectPlaylist = { playlist -> viewModel.addSongToPlaylist(playlist, targetSong) },
                onCreateNewPlaylist = { name ->
                    viewModel.createPlaylist(name = name, initialSongIds = listOf(targetSong.id))
                }
            )
        }

        metadataEditSong?.let { targetSong ->
            MetadataEditorDialog(
                song = targetSong,
                onDismiss = { metadataEditSong = null },
                onSave = { title, artist, album, genre, year ->
                    viewModel.updateSongMetadata(targetSong.id, title, artist, album, genre, year)
                }
            )
        }

        if (showCreatePlaylistDialog) {
            CreateOrRenamePlaylistDialog(
                titleText = "Create Offline Playlist",
                confirmLabel = "Create",
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name, desc -> viewModel.createPlaylist(name, desc) }
            )
        }

        playlistToRename?.let { targetPlaylist ->
            CreateOrRenamePlaylistDialog(
                initialName = targetPlaylist.name,
                initialDescription = targetPlaylist.description,
                titleText = "Edit Playlist",
                confirmLabel = "Save",
                onDismiss = { playlistToRename = null },
                onConfirm = { name, desc ->
                    viewModel.renamePlaylist(targetPlaylist.id, name, desc)
                }
            )
        }

        if (showSleepTimerSheet) {
            SleepTimerBottomSheet(
                sleepTimerState = playbackState.sleepTimer,
                onDismiss = { showSleepTimerSheet = false },
                onStartTimer = viewModel::startSleepTimer,
                onCancelTimer = viewModel::cancelSleepTimer
            )
        }

        if (showPlaybackSpeedSheet) {
            PlaybackSpeedBottomSheet(
                currentSpeed = playbackState.playbackSpeed,
                currentPitch = playbackState.playbackPitch,
                onDismiss = { showPlaybackSpeedSheet = false },
                onApply = viewModel::setPlaybackSpeedAndPitch
            )
        }

        if (showEqualizerSheet) {
            EqualizerBottomSheet(
                equalizerState = equalizerState,
                onDismiss = { showEqualizerSheet = false },
                onUpdateEqualizer = viewModel::updateEqualizer
            )
        }

        if (showManualLyricsDialog) {
            val activeSongTitle = playbackState.currentSong?.title ?: "Track"
            ManualLyricsEditorDialog(
                songTitle = activeSongTitle,
                initialRawLrc = currentLyrics.rawText,
                onDismiss = { showManualLyricsDialog = false },
                onSave = viewModel::saveManualLyrics
            )
        }
        
        editArtworkSong?.let { targetSong ->
            EditArtworkBottomSheet(
                song = targetSong,
                onDismiss = { editArtworkSong = null },
                onSaveArtwork = { bitmap, embed, applyAlbum -> 
                    viewModel.updateCustomArtwork(targetSong, bitmap, embed, applyAlbum) 
                },
                onRemoveArtwork = { applyAlbum ->
                    viewModel.removeCustomArtwork(targetSong, applyAlbum)
                }
            )
        }
    }
}

@Composable
private fun PermissionRationaleOverlay(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atmosphere = com.nocturne.music.designsystem.LocalArtworkAtmosphere.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassPanel(
                shape = RoundedCornerShape(18.dp),
                tintColor = atmosphere.primaryAccent,
                fillAlpha = 0.18f,
                borderAlpha = 0.36f
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = "Local Audio Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = NocturnePalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Grant media access so Nocturne can scan your device's offline music files.",
                style = MaterialTheme.typography.bodySmall,
                color = NocturnePalette.TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(atmosphere.primaryAccent)
                .clickable(onClick = onGrantClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Grant",
                style = MaterialTheme.typography.labelLarge,
                color = NocturnePalette.AmoledBlack
            )
        }
    }
}
