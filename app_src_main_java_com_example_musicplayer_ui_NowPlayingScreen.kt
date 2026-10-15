package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.musicplayer.PlayerUiState
import com.example.musicplayer.Song
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    song: Song,
    state: PlayerUiState,
    position: StateFlow<Long>,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val positionMs by position.collectAsStateWithLifecycle()
    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
        label = "artScale"
    )
    val duration = if (state.durationMs > 0) state.durationMs else song.durationMs
    val isFavorite = song.id in state.favorites

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .bouncyClick(0.85f, onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Close player",
                    tint = R4.Ink,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PLAYING FROM",
                    color = R4.InkDim,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    "Library",
                    color = R4.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(44.dp))
        }

        // Artwork: breathes while the music plays
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Cover(
                song,
                Modifier
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = artScale
                        scaleY = artScale
                    }
                    .shadow(
                        32.dp,
                        RoundedCornerShape(32.dp),
                        ambientColor = R4.Cyan,
                        spotColor = R4.Cyan
                    ),
                RoundedCornerShape(32.dp),
                large = true
            )
        }

        // Title, artist, favorite
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = song,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                        (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 3 })
                },
                label = "titles"
            ) { current ->
                Column {
                    Text(
                        current.title,
                        color = R4.Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        current.artist,
                        color = R4.InkDim,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val heartScale by animateFloatAsState(
                targetValue = if (isFavorite) 1.18f else 1f,
                animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f),
                label = "heart"
            )
            Box(
                Modifier
                    .size(48.dp)
                    .bouncyClick(0.8f, onFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) R4.Cyan else R4.Ink,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SeekBar(positionMs = positionMs, durationMs = duration, onSeek = onSeek)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), color = R4.InkDim, fontSize = 12.sp)
            Text(formatTime(duration), color = R4.InkDim, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        // Controls
        val shuffleTint by animateColorAsState(
            targetValue = if (state.shuffle) R4.Cyan else R4.InkDim,
            animationSpec = tween(250),
            label = "shuffleTint"
        )
        val repeatTint by animateColorAsState(
            targetValue = if (state.repeatMode != Player.REPEAT_MODE_OFF) R4.Cyan else R4.InkDim,
            animationSpec = tween(250),
            label = "repeatTint"
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .bouncyClick(0.8f, onShuffle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = shuffleTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                Modifier
                    .size(60.dp)
                    .bouncyClick(0.85f, onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = R4.Ink,
                    modifier = Modifier.size(40.dp)
                )
            }
            GlowButton(size = 84.dp, onClick = onPlayPause) {
                PlayPauseIcon(isPlaying = state.isPlaying, size = 44.dp)
            }
            Box(
                Modifier
                    .size(60.dp)
                    .bouncyClick(0.85f, onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = R4.Ink,
                    modifier = Modifier.size(40.dp)
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .bouncyClick(0.8f, onRepeat),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                    else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = repeatTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Thin gradient seek bar that thickens and shows a thumb while you drag. */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val playedFraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = playedFraction,
        animationSpec = tween(260, easing = LinearEasing),
        label = "seekFraction"
    )
    val fraction = dragFraction ?: animatedFraction
    val dragging = dragFraction != null

    val thickness by animateDpAsState(
        targetValue = if (dragging) 10.dp else 5.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "seekThickness"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "seekThumb"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val target = (offset.x / widthPx).coerceIn(0f, 1f)
                    onSeek((target * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeek((it * durationMs).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(thickness)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f))
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(thickness)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(R4.Blue, R4.Cyan)))
        )
        Box(
            Modifier
                .offset { IntOffset((fraction * widthPx).roundToInt() - 8.dp.roundToPx(), 0) }
                .size(16.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.White)
        )
    }
}