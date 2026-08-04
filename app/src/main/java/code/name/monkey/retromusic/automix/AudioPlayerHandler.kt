/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import code.name.monkey.retromusic.helper.MusicPlayerRemote

/**
 * Handler central de reproducción e introspección de posición en tiempo real para Milla / Automix.
 */
object AudioPlayerHandler {

    data class PlaybackState(val position: Long)

    /**
     * Proporciona el estado actual del reproductor (incluyendo los milisegundos de reproducción actual).
     */
    val playbackState: PlaybackState
        get() = PlaybackState(MusicPlayerRemote.songProgressMillis.toLong())
}
