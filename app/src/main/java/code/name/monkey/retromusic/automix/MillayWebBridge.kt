/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import code.name.monkey.retromusic.db.SongEntity
import org.json.JSONObject

/**
 * Puente de comunicación bidireccional entre la Web UI de Monochrome (HTML/JS) y el motor DJ nativo en Kotlin de Milla.
 */
class MillayWebBridge(private val context: Context) {

    @JavascriptInterface
    fun playTrack(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val id = json.optLong("id", System.currentTimeMillis())
            val title = json.optString("title", "Desconocido")
            val artist = json.optString("artist", "Artista Desconocido")
            val album = json.optString("album", "Álbum Desconocido")
            val duration = json.optLong("duration", 180000L)
            val streamUrl = json.optString("url", "deezer://track/$id")
            val albumId = json.optLong("albumId", 0L)
            val artistId = json.optLong("artistId", 0L)

            val songEntity = SongEntity(
                playlistCreatorId = 0L,
                id = id,
                title = title,
                trackNumber = 1,
                year = 2026,
                duration = duration,
                data = streamUrl,
                dateModified = System.currentTimeMillis(),
                albumId = albumId,
                albumName = album,
                artistId = artistId,
                artistName = artist,
                composer = "",
                albumArtist = artist,
                bpm = 120f
            )

            AutomixPlayerEngine.getInstance(context).loadAndPlay(songEntity)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
