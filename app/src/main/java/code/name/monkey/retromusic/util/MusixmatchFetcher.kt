package code.name.monkey.retromusic.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MusixmatchFetcher {
    private const val TAG = "MusixmatchFetcher"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var userToken: String? = null

    /**
     * Obtiene un user_token nuevo de Musixmatch
     */
    private suspend fun fetchToken(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val token = json.optJSONObject("message")
                    ?.optJSONObject("body")
                    ?.optString("user_token")
                
                if (!token.isNullOrEmpty() && token != "null") {
                    userToken = token
                    Log.d(TAG, "Musixmatch token obtenido: $token")
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo token MXM", e)
        }
        return@withContext null
    }

    /**
     * Busca las letras con palabra por palabra (wordSynced) y retorna el texto en formato Enhanced LRC.
     */
    suspend fun getEnhancedLrc(track: String, artist: String): String? = withContext(Dispatchers.IO) {
        var token = userToken ?: fetchToken() ?: return@withContext null

        val encodedTrack = URLEncoder.encode(track, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")

        var url = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&q_track=$encodedTrack&q_artist=$encodedArtist&user_language=en&f_subtitle_length_max_deviation=1&subtitle_format=mxm&app_id=web-desktop-app-v1.0&usertoken=$token"

        try {
            var request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            var response = client.newCall(request).execute()
            var bodyStr = response.body?.string() ?: return@withContext null
            var json = JSONObject(bodyStr)

            // Manejar token expirado
            val status = json.optJSONObject("message")?.optJSONObject("header")?.optInt("status_code")
            val hint = json.optJSONObject("message")?.optJSONObject("header")?.optString("hint")
            if (status == 401 && hint == "renew") {
                Log.d(TAG, "Renovando token MXM...")
                token = fetchToken() ?: return@withContext null
                url = "https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?format=json&q_track=$encodedTrack&q_artist=$encodedArtist&user_language=en&f_subtitle_length_max_deviation=1&subtitle_format=mxm&app_id=web-desktop-app-v1.0&usertoken=$token"
                request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .get()
                    .build()
                response = client.newCall(request).execute()
                bodyStr = response.body?.string() ?: return@withContext null
                json = JSONObject(bodyStr)
            }

            val macroCalls = json.optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONObject("macro_calls") ?: return@withContext null

            // Intentar extraer subtitles (wordSynced)
            val subtitlesList = macroCalls.optJSONObject("track.subtitles.get")
                ?.optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONArray("subtitle_list")

            if (subtitlesList != null && subtitlesList.length() > 0) {
                val subtitleBodyStr = subtitlesList.optJSONObject(0)
                    ?.optJSONObject("subtitle")
                    ?.optString("subtitle_body")

                if (!subtitleBodyStr.isNullOrEmpty()) {
                    val subtitleJsonArray = JSONArray(subtitleBodyStr)
                    return@withContext convertToEnhancedLrc(subtitleJsonArray)
                }
            }

            // Fallback a lineSynced si no hay wordSynced
            val lyricsStr = macroCalls.optJSONObject("track.lyrics.get")
                ?.optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONObject("lyrics")
                ?.optString("lyrics_body")

            if (!lyricsStr.isNullOrEmpty()) {
                return@withContext lyricsStr // Standard lyrics as fallback
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MXM lyrics: $e")
        }
        return@withContext null
    }

    /**
     * Convierte el formato [{"text":"line", "time":{"total":0.12}}, ...] de MXM 
     * a Enhanced LRC [mm:ss.xx] <mm:ss.xx> word <mm:ss.xx>
     * Nota: La macro API de Musixmatch a veces devuelve lineas sencillas en subtitles,
     * por lo que intentamos estructurarlo.
     */
    private fun convertToEnhancedLrc(mxmArray: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until mxmArray.length()) {
            val lineObj = mxmArray.getJSONObject(i)
            val text = lineObj.optString("text")
            val timeObj = lineObj.optJSONObject("time") ?: continue
            
            val totalSecs = timeObj.optDouble("total", 0.0)
            val min = (totalSecs / 60).toInt()
            val sec = (totalSecs % 60).toInt()
            val ms = ((totalSecs * 1000) % 1000).toInt() / 10 // hundredths
            
            val timeStr = String.format("[%02d:%02d.%02d]", min, sec, ms)
            
            sb.append(timeStr).append(" ").append(text).append("\n")
        }
        return sb.toString()
    }
}
