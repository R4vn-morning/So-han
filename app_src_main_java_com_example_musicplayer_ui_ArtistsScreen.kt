package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.PlayerUiState
import com.example.musicplayer.Song

data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val songCount: Int
)

@Composable
fun ArtistsScreen(
    state: PlayerUiState,
    onPlay: (Song, List<Song>) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onScrollProgress: (Float) -> Unit = {}
) {
    val listState = rememberLazyListState()
    ReportScroll(listState, onScrollProgress)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    val artists = remember(state.songs) {
        state.songs
            .groupBy { it.artist }
            .map { (artist, songs) ->
                ArtistGroup(
                    name = artist,
                    songs = songs.sortedBy { it.title },
                    songCount = songs.size
                )
            }
            .sortedBy { it.name }
    }
    
    val expandedArtists = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top + 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(key = "header") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .entrance(0)
            ) {
                Text("Artists", color = R4.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${artists.size} artists · ${state.songs.size} songs",
                    color = R4.InkDim,
                    fontSize = 14.sp
                )
            }
        }

        if (!state.loaded || state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = R4.Cyan)
                }
            }
        } else if (artists.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No music found on this phone yet.",
                    color = R4.InkDim,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(48.dp)
                )
            }
        } else {
            items(artists, key = { it.name }) { artist ->
                val isExpanded = expandedArtists[artist.name] ?: false
                
                Column(Modifier.padding(horizontal = 10.dp)) {
                    // Artist header
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .liquidGlass(RoundedCornerShape(16.dp))
                            .bouncyClick(0.96f) {
                                expandedArtists[artist.name] = !isExpanded
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(R4.Cyan, R4.Blue))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = R4.OnAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Column(Modifier.weight(1f)) {
                            Text(
                                artist.name,
                                color = R4.Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                                color = R4.InkDim,
                                fontSize = 13.sp
                            )
                        }
                        
                        Icon(
                            if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = R4.InkDim,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Expanded song list
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            artist.songs.forEach { song ->
                                SongRow(
                                    song = song,
                                    isCurrent = song.id == state.currentSong?.id,
                                    isPlaying = state.isPlaying,
                                    onClick = { onPlay(song, artist.songs) },
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
