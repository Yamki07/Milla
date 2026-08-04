/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Modelo de datos remoto sincronizado con la tabla `track_metadata` en Supabase PostgreSQL.
 */
data class RemoteTrackMetadata(
    val trackId: String,
    val title: String,
    val artist: String,
    val bpm: Float,
    val musicalKey: String,
    val cueOutMs: Long,
    val replayGain: Float,
    val syncedLyrics: String? = null
)

/**
 * Singleton cliente oficial de Supabase para la base de datos descentralizada global de metadatos
 * y mezcla armónica de Milla Automix.
 */
object SupabaseClientManager {

    private const val TAG = "SupabaseClientManager"

    private const val SUPABASE_URL = "https://brgwlyixvgdvzahmsusf.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_4qGbvRV8ArCt3OkFe4mcCQ_r9DpCKM1"

    private const val TABLE_ENDPOINT = "$SUPABASE_URL/rest/v1/track_metadata"

    /**
     * Consulta de forma segura un metadato en la tabla `track_metadata` de Supabase
     * mediante la API REST de PostgREST.
     */
    suspend fun fetchMetadata(trackId: String): RemoteTrackMetadata? = withContext(Dispatchers.IO) {
        if (trackId.isBlank()) return@withContext null
        try {
            val encodedId = URLEncoder.encode(trackId.trim(), "UTF-8")
            val url = URL("$TABLE_ENDPOINT?track_id=eq.$encodedId&select=*")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "RetroMusic-Milla-Automix/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    return@withContext RemoteTrackMetadata(
                        trackId = obj.optString("track_id", trackId),
                        title = obj.optString("title", ""),
                        artist = obj.optString("artist", ""),
                        bpm = obj.optDouble("bpm", 0.0).toFloat(),
                        musicalKey = obj.optString("musical_key", ""),
                        cueOutMs = obj.optLong("cue_out_ms", 0L),
                        replayGain = obj.optDouble("replay_gain", 0.0).toFloat(),
                        syncedLyrics = if (obj.has("synced_lyrics") && !obj.isNull("synced_lyrics")) {
                            obj.optString("synced_lyrics")
                        } else null
                    )
                }
            } else {
                Log.w(TAG, "fetchMetadata code=$responseCode para trackId=$trackId")
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchMetadata error (offline o timeout): ${e.message}")
        }
        null
    }

    /**
     * Realiza un upsert (insertar o actualizar) silencioso en Supabase para enriquecer la
     * base de datos global tan pronto la app analice una canción o descargue un tema.
     */
    suspend fun uploadMetadata(metadata: RemoteTrackMetadata) = withContext(Dispatchers.IO) {
        if (metadata.trackId.isBlank()) return@withContext
        try {
            val url = URL(TABLE_ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                setRequestProperty("User-Agent", "RetroMusic-Milla-Automix/1.0")
            }

            val jsonBody = JSONObject().apply {
                put("track_id", metadata.trackId)
                put("title", metadata.title)
                put("artist", metadata.artist)
                put("bpm", metadata.bpm.toDouble())
                put("musical_key", metadata.musicalKey)
                put("cue_out_ms", metadata.cueOutMs)
                put("replay_gain", metadata.replayGain.toDouble())
                if (metadata.syncedLyrics != null) {
                    put("synced_lyrics", metadata.syncedLyrics)
                } else {
                    put("synced_lyrics", JSONObject.NULL)
                }
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Log.d(TAG, "uploadMetadata exitoso para trackId=${metadata.trackId}")
            } else {
                Log.w(TAG, "uploadMetadata respondió HTTP $responseCode para ${metadata.trackId}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "uploadMetadata silencioso (sin internet o servidor no disponible): ${e.message}")
        }
    }
}
