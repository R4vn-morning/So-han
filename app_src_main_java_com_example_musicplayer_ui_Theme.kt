package com.example.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** R4Vn palette: deep night blue, aurora cyan, blue and mint. */
object R4 {
    val Bg = Color(0xFF050B12)
    val Surface = Color(0xFF0B1A24)
    val Cyan = Color(0xFF2DD4E8)
    val Blue = Color(0xFF3B9EFF)
    val Mint = Color(0xFF6EE7B7)
    val Ink = Color(0xFFF2F8FB)
    val InkDim = Color(0xFF9DB4C0)
    val OnAccent = Color(0xFF031318)
}

private val R4Scheme = darkColorScheme(
    primary = R4.Cyan,
    secondary = R4.Blue,
    tertiary = R4.Mint,
    background = R4.Bg,
    surface = R4.Surface,
    onPrimary = R4.OnAccent,
    onBackground = R4.Ink,
    onSurface = R4.Ink,
    onSurfaceVariant = R4.InkDim
)

@Composable
fun MusicPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = R4Scheme, content = content)
}