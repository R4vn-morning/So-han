package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.Song

fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = R4.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) {
            Text(
                action,
                color = R4.Cyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .bouncyClick(onClick = onAction)
                    .padding(8.dp)
            )
        }
    }
}

/** Animated play/pause glyph that morphs with a quick scale and fade. */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, size: Dp, tint: Color = R4.Ink) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(initialScale = 0.5f, animationSpec = tween(200))) togetherWith
                (fadeOut(tween(120)) + scaleOut(targetScale = 0.5f, animationSpec = tween(120)))
        },
        label = "playPause"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

/** Round glass button with a cyan ring and glow. Used for the main play control. */
@Composable
fun GlowButton(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .bouncyClick(0.92f, onClick)
            .shadow(22.dp, CircleShape, ambientColor = R4.Cyan, spotColor = R4.Cyan)
            .glass(CircleShape, tint = R4.Cyan.copy(alpha = 0.20f), strokeWidth = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    val surface = if (primary) {
        Modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(R4.Cyan, R4.Blue)))
    } else {
        Modifier.glass(shape)
    }
    val contentColor = if (primary) R4.OnAccent else R4.Ink
    Row(
        modifier
            .height(52.dp)
            .bouncyClick(0.95f, onClick)
            .then(surface)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/**
 * Three bars shown on the song that is currently selected.
 * They only animate while music is playing, so a paused list costs nothing.
 */
@Composable
fun EqualizerBars(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    if (playing) {
        val transition = rememberInfiniteTransition(label = "eq")
        val durations = listOf(520, 380, 450)
        val heights = durations.mapIndexed { index, duration ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(duration, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "bar$index"
            )
        }
        Row(
            modifier.height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            heights.forEach { animated ->
                Bar(fraction = animated.value, color = color)
            }
        }
    } else {
        Row(
            modifier.height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(3) { Bar(fraction = 0.25f, color = color) }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(3.dp)
            .fillMaxHeight(fraction)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBg by animateColorAsState(
        targetValue = if (isCurrent) R4.Cyan.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(300),
        label = "rowBackground"
    )
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClick(0.97f, onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(rowBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            Cover(
                song,
                Modifier.fillMaxSize(),
                RoundedCornerShape(14.dp),
                large = false,
                crossfade = false
            )
            if (isCurrent) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerBars(playing = isPlaying, color = R4.Cyan)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) R4.Cyan else R4.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                color = R4.InkDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(formatTime(song.durationMs), color = R4.InkDim, fontSize = 12.sp)
    }
}