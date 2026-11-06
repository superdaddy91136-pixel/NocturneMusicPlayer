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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nocturne.music.ui.nowplaying.LyricsFocusScreen
import com.nocturne.music.ui.search.SearchScreen
import com.nocturne.music.ui.settings.AppIconSelectionScreen
import com.nocturne.music.ui.settings.SettingsScreen
import com.nocturne.music.ui.viewmodel.NocturneViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: NocturneViewModel = hiltViewModel()
            val atmosphere by viewModel.artworkAtmosphere.collectAsStateWithLifecycle()
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            
            val requiredAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasAudioPermission = ContextCompat.checkSelfPermission(
                this,
                requiredAudioPermission
            ) == PackageManager.PERMISSION_GRANTED
            
            splashScreen.setKeepOnScreenCondition {
                hasAudioPermission && !userSettings.hasCompletedInitialScan
            }

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
    val pinnedAlbums by viewModel.pinnedAlbums.collectAsStateWithLifecycle()
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
    var isAppIconPickerOpen by rememberSaveable { mutableStateOf(false) }
    var isRecentsOpen by rememberSaveable { mutableStateOf(false) }
    var isProfileOpen by rememberSaveable { mutableStateOf(false) }
    var isBackupOpen by rememberSaveable { mutableStateOf(false) }
    var externalLibraryDrillDown by remember { mutableStateOf<LibraryDrillDown?>(null) }
    var previousDestinationBeforeDrillDown by remember { mutableStateOf<PrimaryDestination?>(null) }
    var isLibraryDrillDownActive by remember { mutableStateOf(false) }

    val openExternalDrillDown: (LibraryDrillDown) -> Unit = { drillDown ->
        if (currentDestination != PrimaryDestination.LIBRARY) {
            previousDestinationBeforeDrillDown = currentDestination
        }
        externalLibraryDrillDown = drillDown
        currentDestination = PrimaryDestination.LIBRARY
    }

    val clearExternalDrillDown: () -> Unit = {
        externalLibraryDrillDown = null
        previousDestinationBeforeDrillDown?.let { prev ->
            currentDestination = prev
            previousDestinationBeforeDrillDown = null
        }
    }

    val isSecondaryScreenOpen = isSettingsOpen ||
            isAppIconPickerOpen ||
            isRecentsOpen ||
            isProfileOpen ||
            isBackupOpen ||
            externalLibraryDrillDown != null ||
            (currentDestination == PrimaryDestination.LIBRARY && isLibraryDrillDownActive)

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

    val isShowingSplash = hasAudioPermission && !userSettings.hasCompletedInitialScan
    val showMiniPlayerAndBottomNav = !isNowPlayingExpanded && !isSecondaryScreenOpen && !isShowingSplash

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[requiredAudioPermission] == true ||
            ContextCompat.checkSelfPermission(context, requiredAudioPermission) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = granted
        if (granted) {
            if (!userSettings.hasCompletedInitialScan) {
                viewModel.scanMediaLibrary()
            }
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            if (!userSettings.hasCompletedInitialScan) {
                viewModel.scanMediaLibrary()
            }
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

    BackHandler(enabled = isNowPlayingExpanded || isAppIconPickerOpen || isSettingsOpen || isRecentsOpen || isProfileOpen || isBackupOpen) {
        when {
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            isAppIconPickerOpen -> isAppIconPickerOpen = false
            isSettingsOpen -> isSettingsOpen = false
            isRecentsOpen -> isRecentsOpen = false
            isProfileOpen -> isProfileOpen = false
            isBackupOpen -> isBackupOpen = false
        }
    }

    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NocturnePalette.AmoledBlack)
    ) {
        val songColor by viewModel.currentSongColor.collectAsStateWithLifecycle()

        androidx.compose.material3.ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Black.copy(alpha = 0.6f),
            drawerContent = {
                androidx.compose.material3.ModalDrawerSheet(
                    drawerContainerColor = NocturnePalette.CharcoalElevated,
                    drawerContentColor = NocturnePalette.TextPrimary,
                    modifier = Modifier.width(300.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .statusBarsPadding()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(NocturnePalette.ObsidianSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userSettings.profilePictureUri != null) {
                                    coil.compose.AsyncImage(
                                        model = userSettings.profilePictureUri,
                                        contentDescription = "Profile Picture",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Profile",
                                        tint = NocturnePalette.EmeraldGlow,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = userSettings.profileName,
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        androidx.compose.material3.IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Menu",
                                tint = NocturnePalette.TextSecondary
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = NocturnePalette.GlassBorderHairline)
                    Spacer(modifier = Modifier.height(16.dp))

                    class MenuItem(val icon: ImageVector, val label: String, val action: () -> Unit)

                    val menuItems = listOf(
                        MenuItem(Icons.Rounded.Person, "Edit Profile") { isProfileOpen = true },
                        MenuItem(Icons.Rounded.Save, "Backup & Restore") { isBackupOpen = true },
                        MenuItem(Icons.Rounded.History, "Listening History") { isRecentsOpen = true },
                        MenuItem(Icons.Rounded.Settings, "Settings") { isSettingsOpen = true }
                    )

                    menuItems.forEach { item ->
                        androidx.compose.material3.NavigationDrawerItem(
                            label = { Text(item.label, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
                            icon = { Icon(item.icon, contentDescription = null, tint = NocturnePalette.EmeraldGlow) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    item.action()
                                }
                            },
                            colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = NocturnePalette.TextPrimary,
                                unselectedIconColor = NocturnePalette.EmeraldGlow
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(NocturnePalette.ObsidianSurface.copy(alpha = 0.9f))
                            .border(
                                width = 1.dp,
                                color = NocturnePalette.EmeraldGlow.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_music_note),
                            contentDescription = null,
                            tint = NocturnePalette.EmeraldGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = data.visuals.message,
                            color = NocturnePalette.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            },
            bottomBar = {
                if (showMiniPlayerAndBottomNav) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PersistentMiniPlayer(
                            songColor = songColor,
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
                    isAppIconPickerOpen -> {
                        AppIconSelectionScreen(
                            currentIconId = userSettings.selectedAppIconId,
                            onSelectIcon = { option ->
                                viewModel.setAppIcon(option)
                            },
                            onBack = { isAppIconPickerOpen = false }
                        )
                    }

                    isSettingsOpen -> {
                        SettingsScreen(
                            userSettings = userSettings,
                            equalizerState = equalizerState,
                            excludedFolders = excludedFolders,
                            cachedLyricsCount = cachedLyricsCount,
                            totalSongsCount = songs.size,
                            onBack = { isSettingsOpen = false },
                            onOpenEqualizerSheet = { showEqualizerSheet = true },
                            onOpenAppIconPicker = { isAppIconPickerOpen = true },
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

                    isProfileOpen -> {
                        com.nocturne.music.ui.home.ProfileScreen(
                            userSettings = userSettings,
                            onSaveProfile = { name, pictureUri ->
                                viewModel.updateProfile(name, pictureUri)
                            },
                            onBack = { isProfileOpen = false }
                        )
                    }

                    isBackupOpen -> {
                        com.nocturne.music.ui.home.BackupScreen(
                            onExportBackup = { viewModel.exportBackup() },
                            onImportBackup = { uri -> viewModel.importBackup(uri) },
                            onBack = { isBackupOpen = false }
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
                            userSettings = userSettings,
                            onPlaySongs = viewModel::playSongs,
                            onOpenArtist = { artistId ->
                                openExternalDrillDown(LibraryDrillDown.ArtistDetail(artistId))
                            },
                            onToggleSongFavorite = viewModel::toggleSongFavorite,
                            onOpenTrackContext = { contextMenuSong = it },
                            onRescanLibrary = viewModel::scanMediaLibrary,
                            onOpenRecents = { isRecentsOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onOpenProfile = { coroutineScope.launch { drawerState.open() } },
                            onOpenBackup = { isBackupOpen = true }
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
                            pinnedAlbums = pinnedAlbums,
                            recentlyPlayed = recentlyPlayed,
                            recentlyAdded = recentlyAdded,
                            currentSong = playbackState.currentSong,
                            isPlaying = playbackState.isPlaying,
                            userSettings = userSettings,
                            externalDrillDown = externalLibraryDrillDown,
                            onClearExternalDrillDown = clearExternalDrillDown,
                            onPlaySongs = viewModel::playSongs,
                            onToggleSongFavorite = viewModel::toggleSongFavorite,
                            onToggleAlbumFavorite = viewModel::toggleAlbumFavorite,
                            onToggleAlbumPinned = viewModel::toggleAlbumPinned,
                            onToggleArtistFavorite = viewModel::toggleArtistFavorite,
                            onToggleGridList = { viewModel.setIsLibraryGridView(it) },
                            onToggleLandingGridList = { viewModel.setIsLibraryLandingGridView(it) },
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
                            onOpenSettings = { isSettingsOpen = true },
                            onDrillDownActiveChanged = { isLibraryDrillDownActive = it }
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
                                openExternalDrillDown(LibraryDrillDown.AlbumDetail(albumId))
                            },
                            onOpenArtist = { artistId ->
                                openExternalDrillDown(LibraryDrillDown.ArtistDetail(artistId))
                            },
                            onOpenPlaylist = { playlistId ->
                                openExternalDrillDown(LibraryDrillDown.PlaylistDetail(playlistId))
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
            var focusLyricsMode by remember { mutableStateOf(false) }
            
            Crossfade(
                targetState = focusLyricsMode,
                animationSpec = tween(250),
                label = "nowPlayingFocusCrossfade"
            ) { isFocusMode ->
                if (isFocusMode) {
                    LyricsFocusScreen(
                        songColor = songColor,
                        playbackState = playbackState,
                        lyricsDocument = currentLyrics,
                        isLoadingLyrics = isLoadingLyrics,
                        onCollapse = { isNowPlayingExpanded = false },
                        onExitLyricsMode = { focusLyricsMode = false },
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSeekTo = viewModel::seekTo,
                        onImportLrcClick = { lrcPickerLauncher.launch(arrayOf("*/*")) },
                        onFetchOnlineClick = viewModel::fetchOnlineLyricsForCurrentSong,
                        onOpenTrackContext = { contextMenuSong = it }
                    )
                } else {
                    NowPlayingScreen(
                        songColor = songColor,
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
                            openExternalDrillDown(LibraryDrillDown.AlbumDetail(albumId))
                        },
                        onNavigateToArtist = { artistId ->
                            isNowPlayingExpanded = false
                            openExternalDrillDown(LibraryDrillDown.ArtistDetail(artistId))
                        },
                        onToggleLyricsMode = { focusLyricsMode = true }
                    )
                }
            }
        } // End of ModalNavigationDrawer

        // Custom Splash Screen overlay removed in favor of system splash screen

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
                    openExternalDrillDown(LibraryDrillDown.AlbumDetail(targetSong.albumId))
                },
                onGoToArtist = {
                    isNowPlayingExpanded = false
                    openExternalDrillDown(LibraryDrillDown.ArtistDetail(targetSong.artistId))
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
