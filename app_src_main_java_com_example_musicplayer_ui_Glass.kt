package com.example.musicplayer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val GlassFill: Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.04f))
)

val GlassStroke: Brush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.42f),
        R4.Cyan.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.05f)
    )
)

/**
 * "Liquid glass" surface: a dark translucent base, a soft light gradient
 * and a bright gradient edge that catches the light from the top left.
 */
fun Modifier.glass(
    shape: Shape,
    tint: Color = Color.Transparent,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(R4.Surface.copy(alpha = 0.62f))
    .background(GlassFill)
    .background(tint)
    .border(strokeWidth, GlassStroke, shape)

/** Click with a springy press-in, no ripple. */
fun Modifier.bouncyClick(pressedScale: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Fade and rise in once, staggered by [index]. Does not replay when scrolled back. */
fun Modifier.entrance(index: Int): Modifier = composed {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 70L)
            progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            shown = true
        }
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 48f
    }
}


/** The scrolling content, recorded once so glass bars can draw a blurred copy of what is behind them. */
val LocalBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

private val BlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Put this on the content that scrolls under the glass bars. */
fun Modifier.backdropSource(layer: GraphicsLayer): Modifier = this.drawWithContent {
    layer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
        this@drawWithContent.drawContent()
    }
    drawLayer(layer)
}

/**
 * Real liquid glass: blurs whatever scrolls underneath (Android 12+), then adds a light
 * tint, a soft highlight and a bright edge. Older phones get the normal translucent glass.
 * Only use it on bars that are drawn OUTSIDE the [backdropSource] content.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color = Color.Transparent,
    strokeWidth: Dp = 1.dp,
    blurRadius: Float = 30f
): Modifier = composed {
    val backdrop = LocalBackdrop.current
    val active = backdrop != null && BlurSupported
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val base = this.clip(shape)
    val blurred = if (active && backdrop != null) {
        base
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                blurLayer.renderEffect = BlurEffect(blurRadius, blurRadius, TileMode.Clamp)
                blurLayer.record(
                    this,
                    layoutDirection,
                    IntSize(size.width.toInt(), size.height.toInt())
                ) {
                    translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                }
                drawLayer(blurLayer)
            }
    } else base
    blurred
        .background(R4.Surface.copy(alpha = if (active) 0.30f else 0.62f))
        .background(GlassFill)
        .background(tint)
        .border(strokeWidth, GlassStroke, shape)
}

/**
 * Frosted strip under the status bar. It fades in when the list has scrolled
 * ([progress] 0..1) and fades out softly at its bottom edge, like iOS.
 */
@Composable
fun TopGlassBar(title: String, progress: () -> Float, modifier: Modifier = Modifier) {
    val backdrop = LocalBackdrop.current
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val active = backdrop != null && BlurSupported
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress()
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        0.72f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                if (active && backdrop != null) {
                    blurLayer.renderEffect = BlurEffect(36f, 36f, TileMode.Clamp)
                    blurLayer.record(
                        this,
                        layoutDirection,
                        IntSize(size.width.toInt(), size.height.toInt())
                    ) {
                        translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                    }
                    drawLayer(blurLayer)
                }
                drawRect(R4.Bg.copy(alpha = if (active) 0.45f else 0.85f))
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
            }
            .statusBarsPadding()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            title,
            color = R4.Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Reports how far a list has scrolled (0 = at top, 1 = past the large title) to the glass top bar. */
@Composable
fun ReportScroll(listState: LazyListState, onProgress: (Float) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 80f).coerceIn(0f, 1f)
        }.collect { onProgress(it) }
    }
}