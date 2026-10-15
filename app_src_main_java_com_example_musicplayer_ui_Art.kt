package com.example.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import com.example.musicplayer.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads and caches album art in three sizes:
 * small (list rows), large (player, cards) and tiny (blurred backdrop).
 * Small bitmaps keep long lists light, which is what keeps scrolling smooth.
 */
object ArtCache {
    private const val SMALL = 192
    private const val LARGE = 600

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8L).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = HashSet<Long>()

    private fun key(song: Song, large: Boolean) = "${song.id}:${if (large) "L" else "S"}"
    private fun ambientKey(song: Song?) = if (song == null) "default:A" else "${song.id}:A"

    fun peek(song: Song, large: Boolean): Bitmap? = cache.get(key(song, large))

    fun peekAmbient(song: Song?): Bitmap? = cache.get(ambientKey(song))

    suspend fun load(context: Context, song: Song, large: Boolean): Bitmap? =
        withContext(Dispatchers.IO) {
            val k = key(song, large)
            val cached = cache.get(k)
            if (cached != null) return@withContext cached
            val isMissing = synchronized(missing) { song.id in missing }
            if (isMissing) return@withContext null

            val bitmap = fetch(context, song, if (large) LARGE else SMALL)
            if (bitmap != null) {
                cache.put(k, bitmap)
            } else {
                synchronized(missing) { missing.add(song.id) }
            }
            bitmap
        }

    /** A 40x40 copy of the cover. Scaled up it looks blurred, and costs the GPU almost nothing. */
    suspend fun loadAmbient(context: Context, song: Song?): Bitmap =
        withContext(Dispatchers.IO) {
            val k = ambientKey(song)
            val cached = cache.get(k)
            if (cached != null) return@withContext cached

            val source: Bitmap? = if (song != null) load(context, song, false) else null
            val base = source
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.default_cover)
            val result = if (base != null) tiny(base) else Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            cache.put(k, result)
            result
        }

    private fun tiny(bitmap: Bitmap): Bitmap {
        val software =
            if (Build.VERSION.SDK_INT >= 26 && bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
        val step = Bitmap.createScaledBitmap(software, 160, 160, true)
        return Bitmap.createScaledBitmap(step, 40, 40, true)
    }

    private fun fetch(context: Context, song: Song, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                return context.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
            } catch (e: Exception) {
                // fall through to the next strategy
            }
        }
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, song.uri)
                val bytes = retriever.embeddedPicture
                if (bytes != null) return decodeSampled(bytes, size)
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            // fall through
        }
        try {
            val art = song.albumArtUri
            if (art != null) {
                context.contentResolver.openInputStream(art)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) return downscale(decoded, size)
                }
            }
        } catch (e: Exception) {
            // no art available
        }
        return null
    }

    private fun decodeSampled(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return downscale(decoded, target)
    }

    private fun downscale(bitmap: Bitmap, target: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= target) return bitmap
        val ratio = target.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}

@Composable
fun rememberCover(song: Song?, large: Boolean): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val id = song?.id
    var bitmap by remember(id, large) {
        mutableStateOf(
            if (song != null) ArtCache.peek(song, large)?.asImageBitmap() else null
        )
    }
    LaunchedEffect(id, large) {
        if (song != null && bitmap == null) {
            bitmap = ArtCache.load(context, song, large)?.asImageBitmap()
        }
    }
    return bitmap
}

@Composable
private fun rememberAmbient(song: Song?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val id = song?.id
    var bitmap by remember(id) {
        mutableStateOf(ArtCache.peekAmbient(song)?.asImageBitmap())
    }
    LaunchedEffect(id) {
        if (bitmap == null) {
            bitmap = ArtCache.loadAmbient(context, song).asImageBitmap()
        }
    }
    return bitmap
}

/**
 * Album cover. Use [large] for the player and big cards, and turn [crossfade] off
 * inside long lists where every row would otherwise carry an animation.
 */
@Composable
fun Cover(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    large: Boolean = false,
    crossfade: Boolean = true
) {
    val bitmap = rememberCover(song, large)
    if (crossfade) {
        Crossfade(
            targetState = bitmap,
            modifier = modifier.clip(shape),
            animationSpec = tween(400),
            label = "cover"
        ) { art -> CoverImage(art) }
    } else {
        Box(modifier.clip(shape)) { CoverImage(bitmap) }
    }
}

@Composable
private fun CoverImage(art: ImageBitmap?) {
    if (art != null) {
        Image(
            bitmap = art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Image(
            painter = painterResource(R.drawable.default_cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Full-screen backdrop: the current cover as a tiny image stretched across the screen,
 * under a night-blue wash. It never animates, so it adds no per-frame work while scrolling.
 */
@Composable
fun AmbientBackground(song: Song?, modifier: Modifier = Modifier) {
    val art = rememberAmbient(song)

    Box(modifier.fillMaxSize().background(R4.Bg)) {
        Crossfade(
            targetState = art,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(900),
            label = "ambientArt"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.55f,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            R4.Bg.copy(alpha = 0.35f),
                            R4.Bg.copy(alpha = 0.80f),
                            R4.Bg
                        )
                    )
                )
        )

        Canvas(Modifier.fillMaxSize()) {
            val radius = size.width * 0.8f
            val topCenter = Offset(size.width * 0.3f, size.height * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(R4.Cyan.copy(alpha = 0.20f), Color.Transparent),
                    center = topCenter,
                    radius = radius
                ),
                radius = radius,
                center = topCenter
            )
            val bottomCenter = Offset(size.width * 0.75f, size.height * 0.95f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(R4.Mint.copy(alpha = 0.12f), Color.Transparent),
                    center = bottomCenter,
                    radius = radius
                ),
                radius = radius,
                center = bottomCenter
            )
        }
    }
}