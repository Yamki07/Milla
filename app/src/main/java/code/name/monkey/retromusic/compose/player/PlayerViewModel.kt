/*
 * Copyright (c) 2026 Milla Automix Engine
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.compose.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.LyricUtil
import code.name.monkey.retromusic.util.LrcParser
import code.name.monkey.retromusic.util.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val _currentSong = MutableStateFlow(Song.emptySong)
    val currentSong: StateFlow<Song> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Progress 0f..1f for the Compose Slider
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // Current position string e.g. "1:23"
    private val _currentPositionText = MutableStateFlow("0:00")
    val currentPositionText: StateFlow<String> = _currentPositionText.asStateFlow()

    // Total duration string e.g. "3:45"
    private val _totalDurationText = MutableStateFlow("0:00")
    val totalDurationText: StateFlow<String> = _totalDurationText.asStateFlow()

    // Whether automix (club mode) is active
    private val _isAutomixActive = MutableStateFlow(false)
    val isAutomixActive: StateFlow<Boolean> = _isAutomixActive.asStateFlow()

    // Shuffle mode (0 = none, 1 = shuffle)
    private val _shuffleMode = MutableStateFlow(0)
    val shuffleMode: StateFlow<Int> = _shuffleMode.asStateFlow()

    // Repeat mode (0 = none, 1 = all, 2 = this)
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Lyrics list
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    init {
        startProgressPolling()
    }

    /** Poll progress from MusicPlayerRemote every 200ms */
    private fun startProgressPolling() {
        viewModelScope.launch {
            while (isActive) {
                val durationMs = MusicPlayerRemote.songDurationMillis
                val positionMs = MusicPlayerRemote.songProgressMillis
                if (durationMs > 0) {
                    _progress.value = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    _currentPositionText.value = MusicUtil.getReadableDurationString(positionMs.toLong())
                    _totalDurationText.value = MusicUtil.getReadableDurationString(durationMs.toLong())
                }
                delay(200)
            }
        }
    }

    fun updateSong(song: Song) {
        _currentSong.value = song
        loadLyricsForSong(song)
    }

    private fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // For Phase 2, we just load offline lyrics if available.
                val file = LyricUtil.getSyncedLyricsFile(song)
                if (file != null && file.exists()) {
                    val content = LyricUtil.getStringFromLrc(file)
                    if (content != null) {
                        val parsed = LrcParser.parse(content)
                        _lyrics.value = parsed
                    } else {
                        _lyrics.value = emptyList()
                    }
                } else {
                    _lyrics.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lyrics.value = emptyList()
            }
        }
    }

    fun updatePlayState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateShuffleMode(mode: Int) {
        _shuffleMode.value = mode
    }

    fun updateRepeatMode(mode: Int) {
        _repeatMode.value = mode
    }

    fun updateAutomixState(active: Boolean) {
        _isAutomixActive.value = active
    }

    fun seekTo(fraction: Float) {
        val durationMs = MusicPlayerRemote.songDurationMillis
        if (durationMs > 0) {
            MusicPlayerRemote.seekTo((fraction * durationMs).toInt())
        }
    }
}
