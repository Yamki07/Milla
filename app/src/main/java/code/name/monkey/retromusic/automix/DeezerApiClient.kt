/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Cliente de red oficial de Milla para Deezer (Búsqueda, ARL Auth Premium y Stream CDN en formato FLAC / MP3 320kbps).
 * Integra autenticación mediante token ARL y consulta a la API de streaming basada en Blowfish/CBC.
 */
object DeezerApiClient {
    private const val TAG = "DeezerApiClient"

    // Token ARL Premium de cuenta anual configurada para streaming de alta fidelidad (FLAC / MP3 320)
    const val ARL_TOKEN = "a897df2da79654aa0e2a791096e7b0a757d05efaef27ef4a5abf6bd57c2a2965896874dca220cf59944041211fe21827eb470ab2f6bb1a088799b00ad8723c1942ffb2fe6898d29157f100ccb2973ee79c3adc73231384e4713ef117e484e0a8"

    private var apiToken: String = ""

    // Cliente OkHttp configurado con la Cookie ARL persistente
    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return listOf(
                    Cookie.Builder()
                        .domain("deezer.com")
                        .path("/")
                        .name("arl")
                        .value(ARL_TOKEN)
                        .httpOnly()
                        .secure()
                        .build()
                )
            }
        })
        .build()

    /**
     * Inicializa la sesión con Deezer obteniendo el `api_token` necesario para streaming/descarga.
     */
    fun initSession(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&api_version=1.0&api_token=null")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val results = json.getJSONObject("results")
                    apiToken = results.getString("checkForm")
                    Log.d(TAG, "Sesión Deezer iniciada con api_token=$apiToken")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al autenticar sesión Deezer con ARL: $e")
                onError(e)
            }
        }.start()
    }

    /**
     * Realiza una búsqueda de canciones en Deezer por palabra clave (título, artista o género).
     */
    fun searchTracks(
        query: String,
        onResult: (List<Song>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        Thread {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val request = Request.Builder()
                    .url("https://api.deezer.com/search?q=$encodedQuery")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val dataArray = json.optJSONArray("data")
                    val songs = mutableListOf<Song>()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
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
                                    data = "deezer://track/$id",
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
                Log.e(TAG, "Error en búsqueda Deezer API: $e")
                onError(e)
            }
        }.start()
    }

    /**
     * Obtiene el enlace de audio directo de alta calidad (FLAC o MP3 320) usando el token de sesión.
     */
    fun fetchStreamUrl(trackId: String, quality: Int = 9, onResult: (String?) -> Unit) {
        if (apiToken.isEmpty()) {
            initSession(
                onSuccess = { fetchStreamUrlInternal(trackId, quality, onResult) },
                onError = { onResult(null) }
            )
        } else {
            fetchStreamUrlInternal(trackId, quality, onResult)
        }
    }

    private fun fetchStreamUrlInternal(trackId: String, quality: Int, onResult: (String?) -> Unit) {
        Thread {
            try {
                val url = "https://www.deezer.com/ajax/gw-light.php?method=song.getData&api_version=1.0&api_token=$apiToken"
                val jsonPayload = JSONObject().apply {
                    put("sng_id", trackId)
                }

                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val resString = response.body?.string() ?: ""
                    val json = JSONObject(resString)
                    val results = json.getJSONObject("results")
                    val trackToken = results.getString("TRACK_TOKEN")

                    val cdnUrl = "https://media.deezer.com/v1/get_url"
                    val cdnPayload = JSONObject().apply {
                        put("license_token", apiToken)
                        put("media", JSONObject().apply {
                            put("type", "FULL")
                            put("formats", listOf(JSONObject().apply {
                                put("cipher", "BF_CBC_STRIPE")
                                put("format", if (quality == 9) "FLAC" else "MP3_320")
                            }))
                        })
                        put("track_tokens", listOf(trackToken))
                    }

                    val cdnBody = cdnPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val cdnRequest = Request.Builder()
                        .url(cdnUrl)
                        .post(cdnBody)
                        .build()

                    client.newCall(cdnRequest).execute().use { cdnResponse ->
                        val cdnRes = JSONObject(cdnResponse.body?.string() ?: "")
                        val dataArray = cdnRes.getJSONArray("data")
                        if (dataArray.length() > 0) {
                            val mediaUrl = dataArray.getJSONObject(0)
                                .getJSONArray("media")
                                .getJSONObject(0)
                                .getJSONArray("sources")
                                .getJSONObject(0)
                                .getString("url")
                            onResult(mediaUrl)
                        } else {
                            onResult(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo URL de audio para trackId=$trackId: $e")
                onResult(null)
            }
        }.start()
    }
}

/**
 * Convierte un modelo Song de RetroMusic a SongEntity para la base de datos de Automix.
 */
fun Song.toSongEntity(): SongEntity {
    return SongEntity(
        playlistCreatorId = 0L,
        id = this.id,
        title = this.title,
        trackNumber = this.trackNumber,
        year = this.year,
        duration = this.duration,
        data = this.data,
        dateModified = this.dateModified,
        albumId = this.albumId,
        albumName = this.albumName,
        artistId = this.artistId,
        artistName = this.artistName,
        composer = this.composer,
        albumArtist = this.albumArtist,
        bpm = 120f
    )
}
