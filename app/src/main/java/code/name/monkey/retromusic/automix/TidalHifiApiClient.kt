/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Cliente nativo para interactuar con la hifi-api de Tidal (Hi-Res Lossless FLAC 24-bit/192kHz).
 */
object TidalHifiApiClient {
    private const val TAG = "TidalHifiApiClient"
    
    // Instancia por defecto de la API HiFi (con fallback a BiniLossless / qqdl)
    private var baseUrl: String = "https://hifi-api.qqdl.site"

    private val client = OkHttpClient.Builder().build()

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /**
     * Busca pistas en Tidal a través de la hifi-api.
     */
    fun searchTracks(
        query: String,
        onResult: (List<Song>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        Thread {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$baseUrl/search/?s=$encodedQuery&limit=25"
                val request = Request.Builder().url(url).get().build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val dataObj = json.optJSONObject("data")
                    val itemsArray = dataObj?.optJSONArray("items")
                    val songs = mutableListOf<Song>()

                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val item = itemsArray.getJSONObject(i)
                            val id = item.optLong("id", -1L)
                            if (id <= 0L) continue

                            val title = item.optString("title", "Desconocido")
                            val durationSec = item.optLong("duration", 0L)
                            val artistObj = item.optJSONObject("artist")
                            val artistName = artistObj?.optString("name", "Artista Desconocido") ?: "Artista Desconocido"
                            val artistId = artistObj?.optLong("id", 0L) ?: 0L
                            val albumObj = item.optJSONObject("album")
                            val albumName = albumObj?.optString("title", "Álbum Desconocido") ?: "Álbum Desconocido"
                            val albumId = albumObj?.optLong("id", 0L) ?: 0L

                            songs.add(
                                Song(
                                    id = id,
                                    title = title,
                                    trackNumber = i + 1,
                                    year = 2026,
                                    duration = durationSec * 1000L,
                                    data = "tidal://track/$id",
                                    dateModified = System.currentTimeMillis(),
                                    albumId = albumId,
                                    albumName = albumName,
                                    artistId = artistId,
                                    artistName = artistName,
                                    composer = "",
                                    albumArtist = artistName
                                )
                            )
                        }
                    }
                    onResult(songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en búsqueda Tidal hifi-api: $e")
                onError(e)
            }
        }.start()
    }

    /**
     * Obtiene la URL del manifiesto o archivo directo Hi-Res FLAC (24-bit / 192kHz).
     */
    fun fetchTrackManifest(
        trackId: Long,
        quality: String = "HI_RES_LOSSLESS",
        onResult: (String?, String) -> Unit
    ) {
        Thread {
            try {
                val url = "$baseUrl/track/?id=$trackId&quality=$quality"
                val request = Request.Builder().url(url).get().build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val dataObj = json.optJSONObject("data")

                    val manifestB64 = dataObj?.optString("manifest", "") ?: ""
                    val audioQuality = dataObj?.optString("audioQuality", quality) ?: quality

                    if (manifestB64.isNotEmpty()) {
                        onResult(manifestB64, audioQuality)
                    } else {
                        onResult(null, audioQuality)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo manifiesto Tidal Hi-Res para id=$trackId: $e")
                onResult(null, quality)
            }
        }.start()
    }
}
