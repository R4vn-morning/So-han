package com.example.musicplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import kotlin.math.abs

private val tabs = listOf(
    "Home" to Icons.Rounded.Home,
    "Artists" to Icons.Rounded.Person,
    "Library" to Icons.Rounded.LibraryMusic,
    "Settings" to Icons.Rounded.Settings
)

/** Floating glass tab bar. The highlight follows the pager while you swipe. */
@Composable
fun GlassNavBar(
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .liquidGlass(RoundedCornerShape(34.dp))
            .padding(6.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
        val pillShape = RoundedCornerShape(28.dp)

        // the highlight stretches while it travels between tabs, like a drop of liquid
        val travel = position - kotlin.math.floor(position)
        val stretch = 1f + 0.35f * kotlin.math.sin(Math.PI.toFloat() * travel)
        Row(
            Modifier
                .offset(x = tabWidth * position - tabWidth * ((stretch - 1f) / 2f))
                .width(tabWidth * stretch)
                .fillMaxHeight()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        listOf(R4.Cyan.copy(alpha = 0.30f), R4.Blue.copy(alpha = 0.14f))
                    )
                )
                .border(1.dp, R4.Cyan.copy(alpha = 0.55f), pillShape)
        ) {}

        Row(Modifier.fillMaxHeight()) {
            tabs.forEachIndexed { index, (label, icon) ->
                val selected = abs(position - index) < 0.5f
                val tint by animateColorAsState(
                    targetValue = if (selected) R4.Ink else R4.InkDim,
                    animationSpec = tween(250),
                    label = "tabTint$index"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bouncyClick(0.92f) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
                    Text(
                        label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}