package com.example.musicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.PlayerUiState
import com.example.musicplayer.Song
import java.util.Calendar
import kotlin.random.Random

private data class Mix(val title: String, val songs: List<Song>, val tint: Color)

private fun buildMixes(songs: List<Song>): List<Mix> {
    if (songs.size < 3) return emptyList()
    val day = System.currentTimeMillis() / 86_400_000L
    return listOf(
        Mix("Daily Mix", songs.shuffled(Random(day)).take(30), R4.Cyan),
        Mix("New Arrivals", songs.sortedByDescending { it.dateAdded }.take(30), R4.Blue),
        Mix("Late Night", songs.shuffled(Random(day + 7)).take(30), R4.Mint)
    )
}

@Composable
fun HomeScreen(
    state: PlayerUiState,
    userName: String,
    onQueryChange: (String) -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onTogglePlay: () -> Unit,
    onSeeAll: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onScrollProgress: (Float) -> Unit = {}
) {
    val listState = rememberLazyListState()
    ReportScroll(listState, onScrollProgress)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searching = state.query.isNotBlank()
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val searchExpanded = searchOpen || state.query.isNotEmpty()
    val greetingAlpha by animateFloatAsState(
        targetValue = if (searchExpanded) 0f else 1f,
        animationSpec = tween(240),
        label = "greetingAlpha"
    )

    val results = remember(state.songs, state.query) {
        if (searching) state.filteredSongs else emptyList()
    }
    val recents = remember(state.songs, state.recentIds) { state.recentSongs }
    val shelf = remember(state.songs, recents) {
        if (recents.isNotEmpty()) recents else state.songs.sortedByDescending { it.dateAdded }.take(12)
    }
    val mixes = remember(state.songs) { buildMixes(state.songs) }
    val featured = state.currentSong ?: recents.firstOrNull() ?: state.songs.firstOrNull()
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..22 -> "Good evening"
            else -> "Good night"
        }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top + 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .entrance(0)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = greetingAlpha },
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("$greeting,", color = R4.InkDim, fontSize = 15.sp)
                        Text(
                            userName,
                            color = R4.Ink,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Let the music guide your day.", color = R4.InkDim, fontSize = 13.sp)
                    }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(R4.Cyan, R4.Blue)))
                            .border(1.dp, GlassStroke, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            userName.take(1).uppercase(),
                            color = R4.OnAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ExpandableSearch(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    expanded = searchExpanded,
                    onExpandedChange = { searchOpen = it },
                    placeholder = "Search songs, artists, albums...",
                    modifier = Modifier.align(Alignment.TopEnd),
                    restingEndPadding = 60.dp
                )
            }
        }

        if (searching) {
            if (results.isEmpty()) {
                item(key = "noResults") {
                    Text(
                        "No matches.",
                        color = R4.InkDim,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            items(results, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == state.currentSong?.id,
                    isPlaying = state.isPlaying,
                    onClick = { onPlay(song, results) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        } else if (!state.loaded || state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = R4.Cyan)
                }
            }
        } else if (state.songs.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No music found on this phone yet.\nAdd some songs and come back.",
                    color = R4.InkDim,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            }
        } else {
            if (featured != null) {
                item(key = "featured") {
                    val isCurrent = featured.id == state.currentSong?.id
                    FeaturedCard(
                        song = featured,
                        isCurrent = isCurrent,
                        isPlaying = state.isPlaying,
                        onClick = {
                            if (isCurrent) onTogglePlay() else onPlay(featured, state.songs)
                        },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .entrance(2)
                    )
                }
            }

            item(key = "shelf") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.entrance(3)
                ) {
                    SectionHeader(
                        title = if (recents.isNotEmpty()) "Recently Played" else "Recently Added",
                        action = "See all",
                        onAction = onSeeAll
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(shelf, key = { it.id }) { song ->
                            ShelfItem(song = song, onClick = { onPlay(song, shelf) })
                        }
                    }
                }
            }

            if (mixes.isNotEmpty()) {
                item(key = "mixes") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.entrance(4)
                    ) {
                        SectionHeader(title = "Made for You")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(mixes, key = { it.title }) { mix ->
                                MixCard(
                                    mix = mix,
                                    onClick = { onPlay(mix.songs.first(), mix.songs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(210.dp)
            .bouncyClick(0.98f, onClick)
            .clip(shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), RectangleShape, large = true)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, R4.Bg.copy(alpha = 0.88f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(22.dp)
        ) {
            Text(
                if (isCurrent) "NOW PLAYING" else "JUMP BACK IN",
                color = R4.Cyan,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                song.title,
                color = R4.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = R4.InkDim,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(R4.Cyan, R4.Blue))),
            contentAlignment = Alignment.Center
        ) {
            PlayPauseIcon(isPlaying = isCurrent && isPlaying, size = 30.dp, tint = R4.OnAccent)
        }
        Box(Modifier.matchParentSize().border(1.dp, GlassStroke, shape))
    }
}

@Composable
private fun ShelfItem(song: Song, onClick: () -> Unit) {
    Column(
        Modifier
            .width(100.dp)
            .bouncyClick(0.95f, onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Cover(song, Modifier.size(100.dp), RoundedCornerShape(24.dp), large = true, crossfade = false)
        Text(
            song.title,
            color = R4.Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            color = R4.InkDim,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MixCard(mix: Mix, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .width(156.dp)
            .height(196.dp)
            .bouncyClick(0.96f, onClick)
            .clip(shape)
    ) {
        Cover(mix.songs.first(), Modifier.fillMaxSize(), RectangleShape, large = true, crossfade = false)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            mix.tint.copy(alpha = 0.35f),
                            R4.Bg.copy(alpha = 0.92f)
                        )
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                mix.title,
                color = R4.Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${mix.songs.size} songs", color = R4.InkDim, fontSize = 12.sp)
        }
        Box(Modifier.matchParentSize().border(1.dp, GlassStroke, shape))
    }
}