/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Random

/**
 * Cliente de red oficial de Milla para Deezer (Búsqueda, ARL Auth Premium y Stream CDN en formato FLAC / MP3 320kbps).
 * Integra autenticación mediante token ARL y consulta a la API de streaming basada en Blowfish/CBC.
 * Ported from ReFreezer (DJDoubleD) — Dart DeezerAPI → Kotlin.
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

    // ─────────────────────────────────────────────────────────────────
    //  Coroutine-friendly API for MillayFragment (suspend functions)
    //  Ported from ReFreezer deezer.dart callGwApi + search()
    // ─────────────────────────────────────────────────────────────────

    /**
     * Ensures the API token is obtained before making any API call.
     * Matches ReFreezer's rawAuthorize() / authorize() logic.
     */
    private suspend fun ensureToken() {
        if (apiToken.isNotEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&api_version=1.0&api_token=null")
                    .addHeader("Cookie", "arl=$ARL_TOKEN")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext
                    val json = JSONObject(body)
                    apiToken = json.getJSONObject("results").optString("checkForm", "")
                    Log.d(TAG, "Token GW obtenido: $apiToken")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensureToken error: $e")
            }
        }
    }

    /**
     * Search Deezer via public API (does not require ARL token, avoids blocks).
     */
    suspend fun search(query: String): List<DeezerTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQ = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.deezer.com/search?q=$encodedQ&limit=50"
            val request = Request.Builder().url(url).get().build()
            
            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(resBody)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                
                val tracks = mutableListOf<DeezerTrack>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val albumMd5 = item.optJSONObject("album")?.optString("md5_image", "") ?: ""
                    val track = DeezerTrack(
                        id = item.optLong("id").toString(),
                        title = item.optString("title"),
                        artistName = item.optJSONObject("artist")?.optString("name") ?: "Desconocido",
                        albumTitle = item.optJSONObject("album")?.optString("title") ?: "Desconocido",
                        albumCoverId = albumMd5,
                        durationSec = item.optInt("duration", 0),
                        explicit = item.optBoolean("explicit_lyrics", false),
                        md5Origin = item.optString("md5_image", ""),
                        mediaVersion = "",
                        trackToken = "",
                        fileSize320 = 0L,
                        fileSize128 = 0L,
                        fileFlac = 0L
                    )
                    tracks.add(track)
                }
                tracks
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: $e")
            emptyList()
        }
    }

    /**
     * Get Top Tracks from Deezer Charts
     */
    suspend fun getTopTracks(): List<DeezerTrack> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://api.deezer.com/chart/0/tracks?limit=20").get().build()
            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(resBody)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                val tracks = mutableListOf<DeezerTrack>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val albumMd5 = item.optJSONObject("album")?.optString("md5_image", "") ?: ""
                    val track = DeezerTrack(
                        id = item.optLong("id").toString(),
                        title = item.optString("title"),
                        artistName = item.optJSONObject("artist")?.optString("name") ?: "Desconocido",
                        albumTitle = item.optJSONObject("album")?.optString("title") ?: "Desconocido",
                        albumCoverId = albumMd5,
                        durationSec = item.optInt("duration", 0),
                        explicit = item.optBoolean("explicit_lyrics", false),
                        md5Origin = item.optString("md5_image", ""),
                        mediaVersion = "",
                        trackToken = "",
                        fileSize320 = 0L,
                        fileSize128 = 0L,
                        fileFlac = 0L
                    )
                    tracks.add(track)
                }
                tracks
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get Top Albums from Deezer Charts (using a simplified model for the UI)
     */
    suspend fun getTopAlbums(): List<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://api.deezer.com/chart/0/albums?limit=15").get().build()
            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(resBody)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                val albums = mutableListOf<Map<String, String>>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    albums.add(mapOf(
                        "id" to item.optLong("id").toString(),
                        "title" to item.optString("title"),
                        "artist" to (item.optJSONObject("artist")?.optString("name") ?: ""),
                        "cover" to item.optString("cover_xl")
                    ))
                }
                albums
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Resuelve los tokens privados (necesarios para streaming) 
     * consultando la pista individualmente en el API GW-Light.
     */
    private suspend fun fetchPrivateTokens(trackId: String): DeezerTrack? = withContext(Dispatchers.IO) {
        ensureToken()
        val url = "https://www.deezer.com/ajax/gw-light.php?api_version=1.0&api_token=$apiToken&input=3&method=song.getData"
        val payload = JSONObject().apply { put("sng_id", trackId) }
        val body = payload.toString().toRequestBody("text/plain;charset=UTF-8".toMediaType())
        val request = Request.Builder()
            .url(url).post(body)
            .addHeader("Cookie", "arl=$ARL_TOKEN")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        client.newCall(request).execute().use { response ->
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            val data = json.optJSONObject("results") ?: return@withContext null
            DeezerTrack(
                id = data.optString("SNG_ID"),
                title = data.optString("SNG_TITLE"),
                artistName = data.optString("ART_NAME"),
                albumTitle = data.optString("ALB_TITLE"),
                albumCoverId = data.optString("ALB_PICTURE", ""),
                durationSec = data.optInt("DURATION"),
                explicit = data.optString("EXPLICIT_LYRICS", "0") == "1",
                md5Origin = data.optString("MD5_ORIGIN", ""),
                mediaVersion = data.optString("MEDIA_VERSION"),
                trackToken = data.optString("TRACK_TOKEN"),
                fileSize320 = data.optLong("FILESIZE_MP3_320", 0L),
                fileSize128 = data.optLong("FILESIZE_MP3_128", 0L),
                fileFlac = data.optLong("FILESIZE_FLAC", 0L)
            )
        }
    }

    /**
     * Resolve the CDN stream URL for a track.
     * Uses the track's MD5_ORIGIN, MEDIA_VERSION, and TRACK_TOKEN fields
     * to call media.deezer.com and retrieve an encrypted FLAC/MP3 URL.
     * The URL still needs Blowfish CBC decryption via DeezerDecryptor.
     */
    suspend fun getStreamUrl(track: DeezerTrack, preferFlac: Boolean = true): String? = withContext(Dispatchers.IO) {
        try {
            ensureToken()
            
            // Si el track viene del API público, no tiene TRACK_TOKEN ni tamaños. 
            // Buscamos esos datos privados primero.
            val privateTrack = if (track.trackToken.isEmpty()) {
                fetchPrivateTokens(track.id) ?: track
            } else { track }

            val format = if (preferFlac && privateTrack.fileFlac > 0L) "FLAC" else "MP3_320"
            val cdnUrl = "https://media.deezer.com/v1/get_url"
            val cdnPayload = JSONObject().apply {
                put("license_token", apiToken)
                put("media", JSONObject().apply {
                    put("type", "FULL")
                    put("formats", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("cipher", "BF_CBC_STRIPE")
                            put("format", format)
                        })
                    })
                })
                put("track_tokens", org.json.JSONArray().apply {
                    put(privateTrack.trackToken)
                })
            }
            val cdnBody = cdnPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(cdnUrl)
                .post(cdnBody)
                .addHeader("Cookie", "arl=$ARL_TOKEN")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val dataArr = json.optJSONArray("data") ?: return@withContext null
                if (dataArr.length() == 0) return@withContext null
                val mediaArr = dataArr.getJSONObject(0).optJSONArray("media") ?: return@withContext null
                if (mediaArr.length() == 0) return@withContext null
                val sources = mediaArr.getJSONObject(0).optJSONArray("sources") ?: return@withContext null
                if (sources.length() == 0) return@withContext null
                sources.getJSONObject(0).optString("url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getStreamUrl error for track=${track.id}: $e")
            null
        }
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
