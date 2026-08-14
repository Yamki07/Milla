package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.Base64
import java.nio.charset.StandardCharsets

object TidalApiClient {
    private const val TAG = "TidalApiClient"
    
    private val refreshToken: String
        get() = BuildConfig.TIDAL_REFRESH_TOKEN
    private val clientId: String
        get() = BuildConfig.TIDAL_CLIENT_ID
    private val clientSecret: String
        get() = BuildConfig.TIDAL_CLIENT_SECRET
    
    private var accessToken: String = ""
    private var sessionInitialized = false
    var lastError: String? = null

    private val client = OkHttpClient.Builder()
        .connectionSpecs(listOf(okhttp3.ConnectionSpec.MODERN_TLS, okhttp3.ConnectionSpec.COMPATIBLE_TLS, okhttp3.ConnectionSpec.CLEARTEXT))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun ensureSession(forceRefresh: Boolean = false) {
        if (!forceRefresh && sessionInitialized && accessToken.isNotEmpty()) return
        if (refreshToken.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            lastError = "TIDAL no está configurado: completa local.properties con las credenciales de desarrollo."
            Log.e(TAG, lastError ?: "Configuración TIDAL ausente")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("refresh_token", refreshToken)
                    .add("client_id", clientId)
                    .add("grant_type", "refresh_token")
                    .build()

                val authString = "$clientId:$clientSecret"
                val encodedAuth = android.util.Base64.encodeToString(authString.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)

                val request = Request.Builder()
                    .url("https://auth.tidal.com/v1/oauth2/token")
                    .header("Authorization", "Basic $encodedAuth")
                    .header("User-Agent", "Tidal/2.36.1 Android/10")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext
                    if (!response.isSuccessful) {
                        lastError = "Auth Failed HTTP ${response.code}: $body"
                        Log.e(TAG, "Tidal ensureSession Failed HTTP ${response.code}: $body")
                        return@withContext
                    }
                    val json = JSONObject(body)
                    accessToken = json.optString("access_token", "")
                    sessionInitialized = accessToken.isNotEmpty()
                    lastError = null
                    Log.d(TAG, "Tidal Session initialized")
                }
            } catch (e: Exception) {
                lastError = "Auth Error: ${e.message}"
                Log.e(TAG, "Tidal ensureSession error: $e")
            }
        }
    }

    suspend fun search(query: String): List<TidalTrack> = withContext(Dispatchers.IO) {
        try {
            ensureSession()
            if (accessToken.isEmpty()) return@withContext emptyList()

            val encodedQ = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.tidalhifi.com/v1/search?query=$encodedQ&limit=20&types=TRACKS&countryCode=US"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Tidal/2.36.1 Android/10")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(resBody)
                val tracksObj = json.optJSONObject("tracks") ?: return@withContext emptyList()
                val data = tracksObj.optJSONArray("items") ?: return@withContext emptyList()

                val tracks = mutableListOf<TidalTrack>()
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val albumObj = item.optJSONObject("album")
                    val albumCover = albumObj?.optString("cover", "") ?: ""
                    val artistsArr = item.optJSONArray("artists")
                    var artistName = "Unknown"
                    if (artistsArr != null && artistsArr.length() > 0) {
                        artistName = artistsArr.getJSONObject(0).optString("name", "Unknown")
                    }
                    
                    tracks.add(TidalTrack(
                        id = item.optLong("id").toString(),
                        title = item.optString("title"),
                        artistName = artistName,
                        albumTitle = albumObj?.optString("title") ?: "Unknown",
                        albumCoverId = albumCover,
                        durationSec = item.optInt("duration", 0),
                        explicit = item.optBoolean("explicit", false)
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
     * Backward compatibility for MillayHomeFragment
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
                        data = "tidal://track/${track.id}::${track.albumCoverId}",
                        dateModified = System.currentTimeMillis(),
                        albumId = 0L,
                        albumName = track.albumTitle,
                        artistId = 0L,
                        artistName = track.artistName,
                        composer = "tidal",
                        albumArtist = track.artistName
                    )
                }
                onResult(songs)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    suspend fun getStreamUrl(trackId: String, retryCount: Int = 1, quality: String = "HIGH"): String? = withContext(Dispatchers.IO) {
        try {
            ensureSession()
            if (accessToken.isEmpty()) return@withContext null

            val url = "https://api.tidalhifi.com/v1/tracks/$trackId/playbackinfopostpaywall?audioquality=$quality&playbackmode=STREAM&assetpresentation=FULL&countryCode=US"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Tidal/2.36.1 Android/10")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext null
                if (response.code == 401 && retryCount > 0) {
                    Log.d(TAG, "Token expired, refreshing...")
                    ensureSession(forceRefresh = true)
                    return@withContext getStreamUrl(trackId, retryCount - 1, quality)
                }
                if (!response.isSuccessful) {
                    lastError = "Stream URL Failed HTTP ${response.code}: $resBody"
                    Log.e(TAG, "getStreamUrl Failed HTTP ${response.code}: $resBody")
                    if (response.code == 403 && quality == "HIGH") {
                        // Attempt fallback to LOW if HIGH is forbidden (common for free/standard tiers)
                        Log.d(TAG, "Fallback to LOW quality")
                        return@withContext getStreamUrl(trackId, retryCount, "LOW")
                    }
                    return@withContext null
                }
                
                val json = JSONObject(resBody)
                val manifestStr = json.optString("manifest")
                if (manifestStr.isNotEmpty()) {
                    try {
                        // Fix padding for strict Android Base64 decoders
                        val paddedManifestStr = manifestStr + "=".repeat((4 - manifestStr.length % 4) % 4)
                        val decodedBytes = android.util.Base64.decode(paddedManifestStr, android.util.Base64.URL_SAFE or android.util.Base64.DEFAULT)
                        val decodedManifest = String(decodedBytes, StandardCharsets.UTF_8)
                        val manifestJson = JSONObject(decodedManifest)
                        val urls = manifestJson.optJSONArray("urls")
                        if (urls != null && urls.length() > 0) {
                            lastError = null
                            return@withContext urls.getString(0)
                        } else {
                            lastError = "No URLs in manifest"
                        }
                    } catch (e: Exception) {
                        lastError = "Manifest Parse Error: ${e.message}"
                        Log.e(TAG, "Failed to parse manifest: $e")
                    }
                } else {
                    lastError = "Empty manifest returned by Tidal (Account tier limitation?)"
                    if (quality == "HIGH") {
                        return@withContext getStreamUrl(trackId, retryCount, "LOW")
                    }
                }
                return@withContext null
            }
        } catch (e: Exception) {
            lastError = "Network Error: ${e.message}"
            Log.e(TAG, "getStreamUrl error: $e")
            null
        }
    }
    
    suspend fun getLyrics(trackId: String): String = withContext(Dispatchers.IO) {
        try {
            ensureSession()
            if (accessToken.isEmpty()) return@withContext ""
            
            val url = "https://api.tidalhifi.com/v1/tracks/$trackId/lyrics?countryCode=US"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Tidal/2.36.1 Android/10")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext ""
                val json = JSONObject(resBody)
                return@withContext json.optString("lyrics", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getLyrics error: $e")
            ""
        }
    }
}

data class TidalTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val albumCoverId: String,
    val durationSec: Int,
    val explicit: Boolean
) {
    val coverUrlFull: String
        get() = if (albumCoverId.isNotEmpty()) "https://resources.tidal.com/images/${albumCoverId.replace("-", "/")}/1280x1280.jpg" else ""

    val durationString: String
        get() {
            val mins = durationSec / 60
            val secs = durationSec % 60
            return "$mins:${secs.toString().padStart(2, '0')}"
        }
}
