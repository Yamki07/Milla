package code.name.monkey.retromusic.automix

import android.util.Log
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
    
    // Refresh Token obtenido via OAuth Device Flow
    private const val REFRESH_TOKEN = "eyJraWQiOiJoUzFKYTdVMCIsImFsZyI6IkVTNTEyIn0.eyJ0eXBlIjoibzJfcmVmcmVzaCIsInVpZCI6MjA0MTg4NTU1LCJzY29wZSI6IndfdXNyIHJfdXNyIHdfc3ViIiwiY2lkIjoxMzMxOSwic1ZlciI6MSwiZ1ZlciI6MCwiaXNzIjoiaHR0cHM6Ly9hdXRoLnRpZGFsLmNvbS92MSJ9.ALlkbro7NIpyKNrtjCrh2_lqrxJIMUURSzLCi3KlqY7MTwAV9VO7-O4qbzog8AekvHKFf4l0HWgqD8OJk-YKlS_yAeBdhtxuY8bv_SdAcYdptgXOwYecdgGqIlPdTEobsgbyQ-105AN5Tu24MP8DG7qGgd24kzEmN2fQ5Jfs6A5w8LgH"
    private const val CLIENT_ID = "fX2JxdmntZWK0ixT"
    private const val CLIENT_SECRET = "1Nn9AfDAjxrgJFJbKNWLeAyKGVGmINuXPPLHVXAvxAg="
    
    private var accessToken: String = ""
    private var sessionInitialized = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun ensureSession() {
        if (sessionInitialized && accessToken.isNotEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("refresh_token", REFRESH_TOKEN)
                    .add("client_id", CLIENT_ID)
                    .add("grant_type", "refresh_token")
                    .build()

                val authString = "$CLIENT_ID:$CLIENT_SECRET"
                val encodedAuth = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.util.Base64.getEncoder().encodeToString(authString.toByteArray(StandardCharsets.UTF_8))
                } else {
                    android.util.Base64.encodeToString(authString.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                }

                val request = Request.Builder()
                    .url("https://auth.tidal.com/v1/oauth2/token")
                    .header("Authorization", "Basic $encodedAuth")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext
                    val json = JSONObject(body)
                    accessToken = json.optString("access_token", "")
                    sessionInitialized = accessToken.isNotEmpty()
                    Log.d(TAG, "Tidal Session initialized: ${accessToken.take(10)}...")
                }
            } catch (e: Exception) {
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
    
    suspend fun getStreamUrl(trackId: String): String? = withContext(Dispatchers.IO) {
        try {
            ensureSession()
            if (accessToken.isEmpty()) return@withContext null

            val url = "https://api.tidalhifi.com/v1/tracks/$trackId/playbackinfopostpaywall?audioquality=LOSSLESS&playbackmode=STREAM&assetpresentation=FULL"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val resBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(resBody)
                val manifestStr = json.optString("manifest")
                if (manifestStr.isNotEmpty()) {
                    // Base64 only exists on API 26+, fallback to android.util.Base64 for older devices
                    val decodedBytes = android.util.Base64.decode(manifestStr, android.util.Base64.DEFAULT)
                    val decodedManifest = String(decodedBytes, StandardCharsets.UTF_8)
                    val manifestJson = JSONObject(decodedManifest)
                    val urls = manifestJson.optJSONArray("urls")
                    if (urls != null && urls.length() > 0) {
                        return@withContext urls.getString(0)
                    }
                }
                return@withContext null
            }
        } catch (e: Exception) {
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
