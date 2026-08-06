/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Engine
 * Ported from ReFreezer (DJDoubleD) — Dart → Kotlin
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Cliente de red oficial de Milla para Deezer.
 * Integra autenticación mediante cookie ARL y consulta tanto la API pública
 * como la API privada GW-light para streaming y descarga.
 */
object DeezerApiClient {
    private const val TAG = "DeezerApiClient"

    // ARL Premium — cookie de sesión Deezer
    const val ARL_TOKEN = "24f0c28bb6b2250db18693a312c11451126061c05d33aa8a4dcdeb2f9c8af3c6091ae6ae9ddfc2033399f5dfa66f93cae00ed26d05fecb4e0219f6a134b79b11b73712cb1d025c9789c6f2cbd34db3919b1688798024976afc259b8e526cd47f"

    // Tokens de sesión obtenidos de deezer.getUserData
    private var apiToken: String = ""
    private var licenseToken: String = ""
    private var userId: Long = 0
    private var sessionInitialized = false

    // OkHttp con cookie ARL persistente y timeouts generosos
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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

    // ─────────────────────────────────────────────
    //  Inicialización de sesión (getUserData)
    // ─────────────────────────────────────────────

    /**
     * Inicializa la sesión con Deezer obteniendo:
     * - api_token (checkForm) para llamadas a GW-light
     * - license_token para llamadas a media.deezer.com
     * - userId para verificar que el ARL es válido
     */
    private suspend fun ensureSession() {
        if (sessionInitialized && apiToken.isNotEmpty() && licenseToken.isNotEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.deezer.com/ajax/gw-light.php?method=deezer.getUserData&api_version=1.0&api_token=null")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Accept", "*/*")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext
                    val json = JSONObject(body)
                    val results = json.getJSONObject("results")

                    apiToken = results.optString("checkForm", "")
                    userId = results.optJSONObject("USER")?.optLong("USER_ID", 0) ?: 0

                    // License token is inside USER.OPTIONS
                    val options = results.optJSONObject("USER")?.optJSONObject("OPTIONS")
                    licenseToken = options?.optString("license_token", "") ?: ""

                    sessionInitialized = apiToken.isNotEmpty() && licenseToken.isNotEmpty()

                    Log.d(TAG, "Sesión Deezer iniciada: userId=$userId, apiToken=${apiToken.take(10)}..., licenseToken=${licenseToken.take(10)}..., premium=${userId > 0}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensureSession error: $e")
            }
        }
    }

    /**
     * Llama a un método de la API GW-light de Deezer.
     */
    private suspend fun callGwApi(method: String, params: JSONObject = JSONObject()): JSONObject? =
        withContext(Dispatchers.IO) {
            ensureSession()
            val url = "https://www.deezer.com/ajax/gw-light.php?method=$method&api_version=1.0&api_token=$apiToken&input=3"
            val body = params.toString().toRequestBody("text/plain;charset=UTF-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val resBody = response.body?.string() ?: return@withContext null
                    val json = JSONObject(resBody)
                    json.optJSONObject("results")
                }
            } catch (e: Exception) {
                Log.e(TAG, "callGwApi($method) error: $e")
                null
            }
        }

    // ─────────────────────────────────────────────
    //  Búsqueda pública (no requiere ARL)
    // ─────────────────────────────────────────────

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
                    tracks.add(DeezerTrack(
                        id = item.optLong("id").toString(),
                        title = item.optString("title"),
                        artistName = item.optJSONObject("artist")?.optString("name") ?: "Desconocido",
                        albumTitle = item.optJSONObject("album")?.optString("title") ?: "Desconocido",
                        albumCoverId = albumMd5,
                        durationSec = item.optInt("duration", 0),
                        explicit = item.optBoolean("explicit_lyrics", false),
                        md5Origin = "",
                        mediaVersion = "",
                        trackToken = "",
                        fileSize320 = 0L,
                        fileSize128 = 0L,
                        fileFlac = 0L
                    ))
                }
                tracks
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: $e")
            emptyList()
        }
    }

    /**
     * Backward compatibility for MillayHomeFragment and MillaySearchFragment
     */
    fun searchTracks(
        query: String,
        onResult: (List<Song>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                val results = search(query)
                val songs = results.mapIndexed { index, track ->
                    Song(
                        id = track.id.toLongOrNull() ?: 0L,
                        title = track.title,
                        trackNumber = index + 1,
                        year = 2026,
                        duration = track.durationSec * 1000L,
                        data = "deezer://track/${track.id}",
                        dateModified = System.currentTimeMillis(),
                        albumId = 0L,
                        albumName = track.albumTitle,
                        artistId = 0L,
                        artistName = track.artistName,
                        composer = "",
                        albumArtist = track.artistName
                    )
                }
                onResult(songs)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Charts (Top Tracks / Top Albums)
    // ─────────────────────────────────────────────

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
                    tracks.add(DeezerTrack(
                        id = item.optLong("id").toString(),
                        title = item.optString("title"),
                        artistName = item.optJSONObject("artist")?.optString("name") ?: "Desconocido",
                        albumTitle = item.optJSONObject("album")?.optString("title") ?: "Desconocido",
                        albumCoverId = albumMd5,
                        durationSec = item.optInt("duration", 0),
                        explicit = item.optBoolean("explicit_lyrics", false),
                        md5Origin = "",
                        mediaVersion = "",
                        trackToken = "",
                        fileSize320 = 0L,
                        fileSize128 = 0L,
                        fileFlac = 0L
                    ))
                }
                tracks
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

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

    // ─────────────────────────────────────────────
    //  Datos privados de una pista (song.getData)
    // ─────────────────────────────────────────────

    /**
     * Obtiene los datos privados de la pista (MD5_ORIGIN, MEDIA_VERSION, TRACK_TOKEN, tamaños de archivo).
     * Necesario para construir la URL de stream o la URL de descarga legacy.
     */
    suspend fun fetchPrivateTrackData(trackId: String): DeezerTrack? {
        val results = callGwApi("song.getData", JSONObject().apply { put("sng_id", trackId) })
            ?: return null
        return try {
            DeezerTrack(
                id = results.optString("SNG_ID"),
                title = results.optString("SNG_TITLE"),
                artistName = results.optString("ART_NAME"),
                albumTitle = results.optString("ALB_TITLE"),
                albumCoverId = results.optString("ALB_PICTURE", ""),
                durationSec = results.optInt("DURATION"),
                explicit = results.optString("EXPLICIT_LYRICS", "0") == "1",
                md5Origin = results.optString("MD5_ORIGIN", ""),
                mediaVersion = results.optString("MEDIA_VERSION", ""),
                trackToken = results.optString("TRACK_TOKEN", ""),
                fileSize320 = results.optLong("FILESIZE_MP3_320", 0L),
                fileSize128 = results.optLong("FILESIZE_MP3_128", 0L),
                fileFlac = results.optLong("FILESIZE_FLAC", 0L)
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchPrivateTrackData error: $e")
            null
        }
    }

    // ─────────────────────────────────────────────
    //  Letras
    // ─────────────────────────────────────────────

    /**
     * Obtiene las letras de una pista usando el método song.getLyrics.
     */
    suspend fun getLyrics(trackId: String): String? {
        val results = callGwApi("song.getLyrics", JSONObject().apply { put("sng_id", trackId) })
            ?: return null
        return try {
            results.optString("LYRICS_TEXT", "").takeIf { it.isNotEmpty() } 
                ?: results.optString("LYRICS_SYNC_JSON", "").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "getLyrics error: $e")
            null
        }
    }

    // ─────────────────────────────────────────────
    //  URL de Stream/Descarga — Método media.deezer.com
    // ─────────────────────────────────────────────

    /**
     * Obtiene la URL de stream/descarga vía media.deezer.com/v1/get_url
     * usando el license_token del usuario y el track_token de la pista.
     * @param format "FLAC", "MP3_320", "MP3_128"
     */
    suspend fun getStreamUrl(track: DeezerTrack, format: String = "MP3_320"): String? =
        withContext(Dispatchers.IO) {
            try {
                ensureSession()

                // Si el track no tiene datos privados, los obtenemos
                val privateTrack = if (track.trackToken.isEmpty() || track.md5Origin.isEmpty()) {
                    fetchPrivateTrackData(track.id) ?: track
                } else track

                if (privateTrack.trackToken.isEmpty()) {
                    Log.e(TAG, "No se obtuvo TRACK_TOKEN para trackId=${track.id}")
                    return@withContext null
                }

                if (licenseToken.isEmpty()) {
                    Log.e(TAG, "No hay license_token — sesión no autenticada")
                    return@withContext null
                }

                // Intentar obtener URL via media.deezer.com (método moderno)
                val url = getStreamUrlViaMediaApi(privateTrack, format)
                if (url != null) return@withContext url

                // Fallback: construir URL legacy directamente
                Log.d(TAG, "Fallback: construyendo URL legacy para trackId=${track.id}")
                return@withContext buildLegacyCdnUrl(privateTrack, format)
            } catch (e: Exception) {
                Log.e(TAG, "getStreamUrl error for track=${track.id}: $e")
                null
            }
        }

    /**
     * Método moderno vía media.deezer.com/v1/get_url
     */
    private fun getStreamUrlViaMediaApi(track: DeezerTrack, format: String): String? {
        try {
            val payload = JSONObject().apply {
                put("license_token", licenseToken)
                put("media", JSONArray().put(JSONObject().apply {
                    put("type", "FULL")
                    put("formats", JSONArray().put(JSONObject().apply {
                        put("cipher", "BF_CBC_STRIPE")
                        put("format", format)
                    }))
                }))
                put("track_tokens", JSONArray().put(track.trackToken))
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://media.deezer.com/v1/get_url")
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return null
                Log.d(TAG, "media.deezer.com response: ${resBody.take(500)}")
                val json = JSONObject(resBody)
                val dataArr = json.optJSONArray("data") ?: return null
                if (dataArr.length() == 0) return null

                val firstItem = dataArr.getJSONObject(0)

                // Check for errors
                val errors = firstItem.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val errCode = errors.getJSONObject(0).optInt("code", 0)
                    Log.e(TAG, "media.deezer.com error code=$errCode")
                    return null
                }

                val mediaArr = firstItem.optJSONArray("media") ?: return null
                if (mediaArr.length() == 0) return null
                val sources = mediaArr.getJSONObject(0).optJSONArray("sources") ?: return null
                if (sources.length() == 0) return null
                return sources.getJSONObject(0).optString("url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getStreamUrlViaMediaApi error: $e")
            return null
        }
    }

    /**
     * Construye la URL legacy del CDN de Deezer directamente.
     * Formato: https://e-cdns-proxy-{hash[0]}.dzcdn.net/mobile/1/{encPath}
     * Donde encPath = hex(AES_ECB_encrypt(md5origin/quality/sngId/mediaVersion))
     *
     * Este método NO depende del license_token ni del track_token.
     * Solo requiere MD5_ORIGIN y MEDIA_VERSION del API GW-light.
     */
    private fun buildLegacyCdnUrl(track: DeezerTrack, format: String): String? {
        if (track.md5Origin.isEmpty() || track.mediaVersion.isEmpty()) {
            Log.e(TAG, "No hay MD5_ORIGIN o MEDIA_VERSION para buildLegacyCdnUrl")
            return null
        }

        val qualityCode = when (format) {
            "FLAC" -> "9"
            "MP3_320" -> "3"
            "MP3_128" -> "1"
            else -> "3"
        }

        try {
            // Step format: md5origin¤qualityCode¤trackId¤mediaVersion
            val step1 = "${track.md5Origin}\u00a4${qualityCode}\u00a4${track.id}\u00a4${track.mediaVersion}"
            val md5Hex = md5Hex(step1)
            val step2 = "$md5Hex\u00a4$step1\u00a4"

            // Pad to multiple of 16 bytes
            val padded = step2.padEnd(((step2.length + 15) / 16) * 16, ' ')

            // AES ECB encrypt with the fixed key
            val aesKey = "jo6aey6haid2Teih".toByteArray(Charsets.UTF_8)
            val cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(aesKey, "AES"))
            val encrypted = cipher.doFinal(padded.toByteArray(Charsets.ISO_8859_1))
            val encHex = bytesToHex(encrypted).lowercase()

            val cdnUrl = "https://e-cdns-proxy-${track.md5Origin[0]}.dzcdn.net/mobile/1/$encHex"
            Log.d(TAG, "Legacy CDN URL built: ${cdnUrl.take(80)}...")
            return cdnUrl
        } catch (e: Exception) {
            Log.e(TAG, "buildLegacyCdnUrl error: $e")
            return null
        }
    }

    /**
     * Obtiene las calidades disponibles para una pista.
     * Devuelve una lista de mapas con "format", "label" y "size".
     */
    suspend fun getAvailableQualities(trackId: String): List<Map<String, String>> {
        val track = fetchPrivateTrackData(trackId) ?: return emptyList()
        val qualities = mutableListOf<Map<String, String>>()

        if (track.fileFlac > 0L) {
            qualities.add(mapOf(
                "format" to "FLAC",
                "label" to "FLAC (Lossless)",
                "size" to formatFileSize(track.fileFlac),
                "quality" to "9"
            ))
        }
        if (track.fileSize320 > 0L) {
            qualities.add(mapOf(
                "format" to "MP3_320",
                "label" to "MP3 320kbps",
                "size" to formatFileSize(track.fileSize320),
                "quality" to "3"
            ))
        }
        if (track.fileSize128 > 0L) {
            qualities.add(mapOf(
                "format" to "MP3_128",
                "label" to "MP3 128kbps",
                "size" to formatFileSize(track.fileSize128),
                "quality" to "1"
            ))
        }

        // Si no hay info de tamaños, ofrecer opciones por defecto
        if (qualities.isEmpty()) {
            qualities.add(mapOf("format" to "FLAC", "label" to "FLAC (Lossless)", "size" to "~30MB", "quality" to "9"))
            qualities.add(mapOf("format" to "MP3_320", "label" to "MP3 320kbps", "size" to "~10MB", "quality" to "3"))
            qualities.add(mapOf("format" to "MP3_128", "label" to "MP3 128kbps", "size" to "~5MB", "quality" to "1"))
        }

        return qualities
    }

    // ─────────────────────────────────────────────
    //  Compatibilidad: initSession y fetchStreamUrl legacy
    // ─────────────────────────────────────────────

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
                    val options = results.optJSONObject("USER")?.optJSONObject("OPTIONS")
                    licenseToken = options?.optString("license_token", "") ?: ""
                    userId = results.optJSONObject("USER")?.optLong("USER_ID", 0) ?: 0
                    sessionInitialized = true
                    Log.d(TAG, "Sesión Deezer iniciada (legacy): apiToken=${apiToken.take(10)}...")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al autenticar sesión Deezer con ARL: $e")
                onError(e)
            }
        }.start()
    }

    fun fetchStreamUrl(trackId: String, quality: Int = 3, onResult: (String?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val track = fetchPrivateTrackData(trackId)
                if (track == null) {
                    onResult(null)
                    return@launch
                }
                val format = when (quality) {
                    9 -> "FLAC"
                    3 -> "MP3_320"
                    else -> "MP3_128"
                }
                val url = getStreamUrl(track, format)
                onResult(url)
            } catch (e: Exception) {
                Log.e(TAG, "fetchStreamUrl error: $e")
                onResult(null)
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Utilidades
    // ─────────────────────────────────────────────

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.ISO_8859_1))
        return bytesToHex(digest).lowercase()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "${bytes / 1_000_000}MB"
            bytes >= 1_000 -> "${bytes / 1_000}KB"
            else -> "${bytes}B"
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
