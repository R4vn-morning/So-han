package com.example.musicplayer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.Song
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    durationMs: Long,
    position: StateFlow<Long>,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val positionMs by position.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(
        targetValue = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(300, easing = LinearEasing),
        label = "miniProgress"
    )
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "miniCover"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .bouncyClick(0.98f, onOpen)
            .liquidGlass(RoundedCornerShape(34.dp))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Cover(
                song,
                Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = coverScale
                        scaleY = coverScale
                    },
                CircleShape
            )
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = R4.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = R4.InkDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .bouncyClick(0.85f, onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                PlayPauseIcon(isPlaying = isPlaying, size = 30.dp)
            }
            Box(
                Modifier
                    .size(44.dp)
                    .bouncyClick(0.85f, onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = R4.Ink,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 30.dp)
                .fillMaxWidth(progress)
                .height(2.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(R4.Blue, R4.Cyan)))
        )
    }
}