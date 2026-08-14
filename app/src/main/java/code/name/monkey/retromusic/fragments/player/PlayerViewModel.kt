package code.name.monkey.retromusic.fragments.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentSong: Song = Song.emptySong,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bpm: Float = 0f,
    val mood: String? = null,
    val energy: String? = null,
    val danceability: String? = null
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    init {
        startObservingPlayback()
    }

    private fun startObservingPlayback() {
        viewModelScope.launch {
            while (isActive) {
                val currentSong = MusicPlayerRemote.currentSong
                val isPlaying = MusicPlayerRemote.isPlaying
                val currentPositionMs = MusicPlayerRemote.currentPlaybackPositionMs
                val durationMs = MusicPlayerRemote.songDurationMillis.toLong()
                
                // Milla's MusicService and BpmScanner store BPM/mood somewhere?
                // Milla's Song might have bpm? Wait, Song doesn't have bpm natively unless added.
                // We'll leave bpm as 0f for now if it's not in Song.
                // Or maybe the automix data is accessible from room repo?
                // For UI purposes, we'll just stream the basic song info.
                
                _uiState.value = PlayerState(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs
                )
                
                delay(16L) // ~60fps refresh rate
            }
        }
    }

    fun playPause() {
        if (MusicPlayerRemote.isPlaying) {
            MusicPlayerRemote.pauseSong()
        } else {
            MusicPlayerRemote.resumePlaying()
        }
    }

    fun next() {
        MusicPlayerRemote.playNextSong()
    }

    fun previous() {
        MusicPlayerRemote.back()
    }

    fun seekTo(positionMs: Long) {
        MusicPlayerRemote.seekTo(positionMs.toInt())
    }
}
