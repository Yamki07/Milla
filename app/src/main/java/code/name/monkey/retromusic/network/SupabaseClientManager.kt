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
    val bpm: Float? = null,
    val musicalKey: String? = null,
    val cueOutMs: Long,
    val replayGain: Float,
    val syncedLyrics: String? = null,
    /** JSON completo con todos los datos avanzados (Beats, Energy Curve, Onsets, etc) */
    val fullProfileJson: String? = null,
    val mood: String? = null,
    val halfTimeBpm: Float? = null,
    val mode: String? = null,
    val energy: String? = null,
    val danceability: String? = null,
    val timeSignature: Int? = null,
    val doubleTimeBpm: Float? = null,
    val syncedLyricsTranslated: String? = null
)

/**
 * Singleton cliente oficial de Supabase para la base de datos descentralizada global de metadatos
 * y mezcla armónica de Milla Automix.
 */
object SupabaseClientManager {

    private const val TAG = "SupabaseClientManager"

    private val SUPABASE_URL = code.name.monkey.retromusic.BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY = code.name.monkey.retromusic.BuildConfig.SUPABASE_ANON_KEY

    private val TABLE_ENDPOINT = "$SUPABASE_URL/rest/v1/track_metadata"

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
                        bpm = if (obj.has("bpm") && !obj.isNull("bpm")) obj.optDouble("bpm").toFloat() else null,
                        musicalKey = if (obj.has("musical_key") && !obj.isNull("musical_key")) obj.optString("musical_key") else null,
                        cueOutMs = obj.optLong("cue_out_ms", 0L),
                        replayGain = obj.optDouble("replay_gain", 0.0).toFloat(),
                        syncedLyrics = if (obj.has("synced_lyrics") && !obj.isNull("synced_lyrics")) {
                            obj.optString("synced_lyrics")
                        } else null,
                        fullProfileJson = if (obj.has("full_profile_json") && !obj.isNull("full_profile_json")) {
                            obj.optString("full_profile_json")
                        } else null,
                        mood = if (obj.has("mood") && !obj.isNull("mood")) obj.optString("mood") else null,
                        halfTimeBpm = if (obj.has("half_time_bpm") && !obj.isNull("half_time_bpm")) obj.optDouble("half_time_bpm").toFloat() else null,
                        mode = if (obj.has("mode") && !obj.isNull("mode")) obj.optString("mode") else null,
                        energy = if (obj.has("energy") && !obj.isNull("energy")) obj.optString("energy") else null,
                        danceability = if (obj.has("danceability") && !obj.isNull("danceability")) obj.optString("danceability") else null,
                        timeSignature = if (obj.has("time_signature") && !obj.isNull("time_signature")) obj.optInt("time_signature") else null,
                        doubleTimeBpm = if (obj.has("double_time_bpm") && !obj.isNull("double_time_bpm")) obj.optDouble("double_time_bpm").toFloat() else null,
                        syncedLyricsTranslated = if (obj.has("synced_lyrics_translated") && !obj.isNull("synced_lyrics_translated")) obj.optString("synced_lyrics_translated") else null
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
                metadata.bpm?.takeIf { it > 0f }?.let { put("bpm", it.toDouble()) }
                metadata.musicalKey?.takeIf { it.isNotBlank() }?.let { put("musical_key", it) }
                put("cue_out_ms", metadata.cueOutMs)
                put("replay_gain", metadata.replayGain.toDouble())
                if (metadata.syncedLyrics != null) {
                    put("synced_lyrics", metadata.syncedLyrics)
                } else {
                    put("synced_lyrics", JSONObject.NULL)
                }
                if (metadata.fullProfileJson != null) {
                    put("full_profile_json", metadata.fullProfileJson)
                }
                metadata.mood?.takeIf { it.isNotBlank() }?.let { put("mood", it) }
                metadata.halfTimeBpm?.takeIf { it > 0f }?.let { put("half_time_bpm", it.toDouble()) }
                metadata.mode?.takeIf { it.isNotBlank() }?.let { put("mode", it) }
                metadata.energy?.takeIf { it.isNotBlank() }?.let { put("energy", it) }
                metadata.danceability?.takeIf { it.isNotBlank() }?.let { put("danceability", it) }
                metadata.timeSignature?.takeIf { it > 0 }?.let { put("time_signature", it) }
                metadata.doubleTimeBpm?.takeIf { it > 0f }?.let { put("double_time_bpm", it.toDouble()) }
                if (metadata.syncedLyricsTranslated != null) {
                    put("synced_lyrics_translated", metadata.syncedLyricsTranslated)
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

    suspend fun insertTrackMetadata(data: List<Map<String, Any>>) = withContext(Dispatchers.IO) {
        for (item in data) {
            val trackId = item["track_id"] as? String ?: continue
            val title = item["title"] as? String ?: ""
            val artist = item["artist"] as? String ?: ""
            val bpm = (item["bpm"] as? Number)?.toFloat() ?: 0f
            val key = item["musical_key"] as? String ?: ""
            val cueOut = (item["cue_out_ms"] as? Number)?.toLong() ?: 0L
            val gain = (item["replay_gain"] as? Number)?.toFloat() ?: 0f
            
            val metadata = RemoteTrackMetadata(
                trackId = trackId,
                title = title,
                artist = artist,
                bpm = bpm,
                musicalKey = key,
                cueOutMs = cueOut,
                replayGain = gain
            )
            uploadMetadata(metadata)
        }
    }
}
