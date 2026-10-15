package com.example.musicplayer.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val action: (() -> Unit)? = null
)

@Composable
fun SettingsScreen(
    userName: String,
    songCount: Int,
    onRescan: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onScrollProgress: (Float) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ReportScroll(listState, onScrollProgress)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val entries = listOf(
        SettingsItem(Icons.Rounded.Palette, "Appearance", "Theme, color, glass, animations"),
        SettingsItem(Icons.Rounded.PlayCircle, "Playback", "Shuffle, repeat, gapless"),
        SettingsItem(Icons.Rounded.GraphicEq, "Audio", "Audio focus, output, quality"),
        SettingsItem(Icons.Rounded.LibraryMusic, "Library", "Scan again ($songCount songs found)", onRescan),
        SettingsItem(Icons.Rounded.Notifications, "Notifications", "Playback, system"),
        SettingsItem(Icons.Rounded.Info, "About", "Version 1.0.0")
    )

    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = top + 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Text(
                "Settings",
                color = R4.Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
                    .entrance(0)
            )
        }
        item(key = "profile") {
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .entrance(1)
                    .glass(RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(R4.Cyan, R4.Blue)))
                        .border(1.dp, GlassStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        color = R4.OnAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        userName,
                        color = R4.Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("R4Vn Music", color = R4.InkDim, fontSize = 13.sp)
                }
            }
        }
        entries.forEachIndexed { index, entry ->
            item(key = entry.title) {
                SettingsRow(
                    entry = entry,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .entrance(index + 2)
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(entry: SettingsItem, modifier: Modifier = Modifier) {
    val click = entry.action
    Row(
        modifier
            .fillMaxWidth()
            .then(if (click != null) Modifier.bouncyClick(0.97f, click) else Modifier)
            .glass(RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(R4.Cyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.icon, contentDescription = null, tint = R4.Cyan, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = R4.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(entry.subtitle, color = R4.InkDim, fontSize = 12.sp)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = R4.InkDim,
            modifier = Modifier.size(22.dp)
        )
    }
}