package com.example.musicplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.PlayerUiState
import com.example.musicplayer.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val USER_NAME = "Sabhan"

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun R4vnApp(viewModel: PlayerViewModel, resumeTick: Int) {
    val context = LocalContext.current
    var granted by remember(resumeTick) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission()) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(audioPermission())
    }
    LaunchedEffect(granted, resumeTick) {
        if (granted) {
            viewModel.loadSongs()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!granted) {
        PermissionScreen(
            onAllow = { permissionLauncher.launch(audioPermission()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        )
    } else {
        MainContent(viewModel, state)
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AmbientBackground(null)
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Cover(null, Modifier.size(140.dp), RoundedCornerShape(36.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                "R4Vn Music",
                color = R4.Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text("Your Sound. Your World.", color = R4.InkDim, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                "Allow access to the audio files on your phone so R4Vn can play your music.",
                color = R4.InkDim,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            PillButton(
                text = "Allow access",
                icon = Icons.Rounded.LibraryMusic,
                primary = true,
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "Open settings",
                icon = Icons.Rounded.LibraryMusic,
                primary = false,
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MainContent(viewModel: PlayerViewModel, state: PlayerUiState) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    // 0 = mini player, 1 = full player. Dragging and tapping both drive this value.
    val expansion = remember { Animatable(0f).apply { updateBounds(0f, 1f) } }
    val song = state.currentSong
    val backdrop = rememberGraphicsLayer()
    var homeScroll by remember { mutableFloatStateOf(0f) }
    var artistsScroll by remember { mutableFloatStateOf(0f) }
    var libraryScroll by remember { mutableFloatStateOf(0f) }
    var settingsScroll by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxSize().background(R4.Bg)) {
        val heightPx = with(density) { maxHeight.toPx() }

        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val miniSpace by animateDpAsState(
            targetValue = if (song != null) 78.dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            label = "miniSpace"
        )
        val listBottomPadding = navInset + 12.dp + 68.dp + miniSpace + 24.dp

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                expansion.snapTo((expansion.value - delta / (heightPx * 0.75f)).coerceIn(0f, 1f))
            }
        }
        val dragModifier = Modifier.draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity ->
                val open = velocity < -900f || (velocity < 900f && expansion.value > 0.5f)
                expansion.animateTo(if (open) 1f else 0f, spring(dampingRatio = 1f, stiffness = 300f))
            }
        )

        // Content layer, pushed back slightly while the full player is open
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = expansion.value
                    scaleX = 1f - 0.06f * e
                    scaleY = 1f - 0.06f * e
                    alpha = 1f - 0.5f * e
                }
                .backdropSource(backdrop)
        ) {
            AmbientBackground(song)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offset = abs(
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).coerceIn(0f, 1f)
                            alpha = 1f - 0.65f * offset
                            scaleX = 1f - 0.06f * offset
                            scaleY = 1f - 0.06f * offset
                        }
                ) {
                    when (page) {
                        0 -> HomeScreen(
                            state = state,
                            userName = USER_NAME,
                            onQueryChange = viewModel::setQuery,
                            onPlay = viewModel::play,
                            onTogglePlay = viewModel::togglePlayPause,
                            onSeeAll = { scope.launch { pagerState.animateScrollToPage(2) } },
                            bottomPadding = listBottomPadding,
                            onScrollProgress = { homeScroll = it }
                        )
                        1 -> ArtistsScreen(
                            state = state,
                            onPlay = viewModel::play,
                            bottomPadding = listBottomPadding,
                            onScrollProgress = { artistsScroll = it }
                        )
                        2 -> LibraryScreen(
                            state = state,
                            onQueryChange = viewModel::setQuery,
                            onPlay = viewModel::play,
                            onShuffle = viewModel::shuffleAll,
                            bottomPadding = listBottomPadding,
                            onScrollProgress = { libraryScroll = it }
                        )
                        else -> SettingsScreen(
                            userName = USER_NAME,
                            songCount = state.songs.size,
                            onRescan = viewModel::loadSongs,
                            bottomPadding = listBottomPadding,
                            onScrollProgress = { settingsScroll = it }
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            // Frosted bar under the status bar, appears once the list scrolls
            val topTitle by remember {
                derivedStateOf {
                    val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    when (Math.round(pos)) {
                        0 -> "Home"
                        1 -> "Artists"
                        2 -> "Library"
                        else -> "Settings"
                    }
                }
            }
            TopGlassBar(
                title = topTitle,
                progress = {
                    val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val low = pos.toInt().coerceIn(0, 3)
                    val high = (low + 1).coerceAtMost(3)
                    val t = (pos - low).coerceIn(0f, 1f)
                    val from = when (low) { 0 -> homeScroll; 1 -> artistsScroll; 2 -> libraryScroll; else -> settingsScroll }
                    val to = when (high) { 0 -> homeScroll; 1 -> artistsScroll; 2 -> libraryScroll; else -> settingsScroll }
                    (from + (to - from) * t) * (1f - expansion.value)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

        // Mini player and tab bar float over the content
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val e = expansion.value
                        alpha = 1f - e
                        translationY = e * 160f
                    }
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = song != null,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it }
                ) {
                    if (song != null) {
                        MiniPlayer(
                            song = song,
                            isPlaying = state.isPlaying,
                            durationMs = state.durationMs,
                            position = viewModel.position,
                            onOpen = {
                                scope.launch {
                                    expansion.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
                                }
                            },
                            onPlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::next,
                            modifier = dragModifier
                        )
                    }
                }
                GlassNavBar(
                    pagerState = pagerState,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
                )
            }
        }

        // Full player slides up over everything
        val showPlayer by remember { derivedStateOf { expansion.value > 0.001f } }
        if (showPlayer && song != null) {
            BackHandler {
                scope.launch { expansion.animateTo(0f, spring(dampingRatio = 1f, stiffness = 300f)) }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - expansion.value) * heightPx }
                    .then(dragModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                AmbientBackground(song)
                NowPlayingScreen(
                    song = song,
                    state = state,
                    position = viewModel.position,
                    onClose = {
                        scope.launch {
                            expansion.animateTo(0f, spring(dampingRatio = 1f, stiffness = 300f))
                        }
                    },
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::next,
                    onPrevious = viewModel::previous,
                    onSeek = viewModel::seekTo,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::cycleRepeat,
                    onFavorite = { viewModel.toggleFavorite(song.id) }
                )
            }
        }
    }
}