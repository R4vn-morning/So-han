package com.example.musicplayer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class PlayerUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val loaded: Boolean = false,
    val query: String = "",
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val recentIds: List<Long> = emptyList(),
    val favorites: Set<Long> = emptySet()
) {
    val filteredSongs: List<Song>
        get() = if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }

    val recentSongs: List<Song>
        get() {
            val byId = songs.associateBy { it.id }
            return recentIds.mapNotNull { byId[it] }
        }
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)
    private val prefs = app.getSharedPreferences("r4vn_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        PlayerUiState(
            recentIds = readIds("recent"),
            favorites = readIds("favorites").toSet()
        )
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    // Position changes several times a second, so it lives in its own flow
    // and only the seek bar / mini player listen to it.
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            controller?.let { _position.value = it.currentPosition }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull()
            if (id != null) addRecent(id)
            _state.update { s ->
                val song = if (id != null) s.songs.firstOrNull { it.id == id } else null
                s.copy(
                    currentSong = song ?: s.currentSong,
                    durationMs = controller?.duration?.takeIf { it > 0 }
                        ?: song?.durationMs
                        ?: s.durationMs
                )
            }
            _position.value = 0L
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val d = controller?.duration ?: -1L
                if (d > 0) _state.update { it.copy(durationMs = d) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffle = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode) }
        }
    }

    init {
        connectToService()
        startPositionTicker()
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            syncFromController()
        }, MoreExecutors.directExecutor())
    }

    /** When the app is reopened while music is still playing, pick up where the service is. */
    private fun syncFromController() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
        _state.update { s ->
            val song = if (id != null) s.songs.firstOrNull { it.id == id } else null
            s.copy(
                currentSong = song ?: s.currentSong,
                isPlaying = c.isPlaying,
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                durationMs = c.duration.takeIf { it > 0 } ?: song?.durationMs ?: s.durationMs
            )
        }
        _position.value = c.currentPosition
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    if (c.isPlaying) _position.value = c.currentPosition
                }
                delay(250)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            if (!_state.value.loaded) _state.update { it.copy(isLoading = true) }
            val songs = repository.loadSongs()
            _state.update { it.copy(songs = songs, isLoading = false, loaded = true) }
            syncFromController()
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun play(song: Song, queue: List<Song>) {
        val c = controller ?: return
        val index = queue.indexOfFirst { it.id == song.id }
        if (index < 0) return
        c.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
        _state.update { it.copy(currentSong = song, durationMs = song.durationMs) }
        _position.value = 0L
    }

    fun shuffleAll(queue: List<Song>) {
        val c = controller ?: return
        if (queue.isEmpty()) return
        c.shuffleModeEnabled = true
        play(queue.random(), queue)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFavorite(songId: Long) {
        val updated = _state.value.favorites.let {
            if (songId in it) it - songId else it + songId
        }
        _state.update { it.copy(favorites = updated) }
        writeIds("favorites", updated.toList())
    }

    private fun addRecent(id: Long) {
        val updated = (listOf(id) + _state.value.recentIds.filter { it != id }).take(12)
        _state.update { it.copy(recentIds = updated) }
        writeIds("recent", updated)
    }

    private fun readIds(key: String): List<Long> =
        prefs.getString(key, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }

    private fun writeIds(key: String, ids: List<Long>) {
        prefs.edit().putString(key, ids.joinToString(",")).apply()
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
}