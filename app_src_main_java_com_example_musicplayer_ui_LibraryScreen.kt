package com.example.musicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.PlayerUiState
import com.example.musicplayer.Song

@Composable
fun LibraryScreen(
    state: PlayerUiState,
    onQueryChange: (String) -> Unit,
    onPlay: (Song, List<Song>) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onScrollProgress: (Float) -> Unit = {}
) {
    val listState = rememberLazyListState()
    ReportScroll(listState, onScrollProgress)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val songs = remember(state.songs, state.query) { state.filteredSongs }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val searchExpanded = searchOpen || state.query.isNotEmpty()
    val titleAlpha by animateFloatAsState(
        targetValue = if (searchExpanded) 0f else 1f,
        animationSpec = tween(240),
        label = "libraryTitleAlpha"
    )

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top + 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .entrance(0)
            ) {
                Column(Modifier.graphicsLayer { alpha = titleAlpha }) {
                    Text("Library", color = R4.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.songs.size} songs on this phone",
                        color = R4.InkDim,
                        fontSize = 14.sp
                    )
                }
                ExpandableSearch(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    expanded = searchExpanded,
                    onExpandedChange = { searchOpen = it },
                    placeholder = "Search songs or artists",
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        item(key = "actions") {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
                    .entrance(1),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillButton(
                    text = "Play",
                    icon = Icons.Rounded.PlayArrow,
                    primary = true,
                    onClick = { songs.firstOrNull()?.let { onPlay(it, songs) } },
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = "Shuffle",
                    icon = Icons.Rounded.Shuffle,
                    primary = false,
                    onClick = { onShuffle(songs) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!state.loaded || state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = R4.Cyan)
                }
            }
        } else if (songs.isEmpty()) {
            item(key = "empty") {
                Text(
                    if (state.query.isBlank()) "No music found on this phone yet." else "No matches.",
                    color = R4.InkDim,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp)
                )
            }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == state.currentSong?.id,
                    isPlaying = state.isPlaying,
                    onClick = { onPlay(song, songs) },
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }
}