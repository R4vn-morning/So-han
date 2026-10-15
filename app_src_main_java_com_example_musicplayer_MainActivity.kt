package com.example.musicplayer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.musicplayer.ui.MusicPlayerTheme
import com.example.musicplayer.ui.R4vnApp

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    // Bumped on every resume so the app re-checks permission and rescans the library.
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighRefreshRate()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MusicPlayerTheme {
                R4vnApp(viewModel, resumeTick)
            }
        }
    }

    /** Asks the system for the highest refresh rate this screen supports (90, 120 Hz...). */
    @Suppress("DEPRECATION")
    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val display = windowManager.defaultDisplay
            val current = display.mode
            val best = display.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight
                }
                .maxByOrNull { it.refreshRate } ?: return
            val params = window.attributes
            params.preferredDisplayModeId = best.modeId
            params.preferredRefreshRate = best.refreshRate
            window.attributes = params
        } catch (e: Exception) {
            // keep the system default
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}