/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import code.name.monkey.retromusic.network.RemoteTrackMetadata
import code.name.monkey.retromusic.network.SupabaseClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Worker de WorkManager que actua como "Seeder" de la base de datos global en Supabase.
 * Crawlea los Top Charts de Deezer y los siembra en Supabase (Cerebro Fantasma).
 */
class MillaySupabaseSeeder(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MillaySupabaseSeeder"
        private const val WORK_NAME = "millay_supabase_seeder"

        private val CHART_ENDPOINTS = mapOf(
            "Top Global"    to "chart/0/tracks?limit=100",
            "Top USA"       to "chart/23/tracks?limit=100",
            "Top Espania"   to "chart/116/tracks?limit=100",
            "Top Francia"   to "chart/4/tracks?limit=100",
            "Top Brasil"    to "chart/8/tracks?limit=100",
            "Top Argentina" to "chart/20/tracks?limit=100",
            "Top Mexico"    to "chart/152/tracks?limit=100",
            "Top Colombia"  to "chart/80/tracks?limit=100",
        )

        private val EDITORIAL_QUERIES = listOf(
            "reggaeton 2025", "pop hits 2024 2025", "k-pop hits 2025",
            "j-pop 2025", "hip hop 2025", "latin hits 2025",
            "grammy 2024 nominations", "grammy 2025 winners",
            "afrobeats 2025", "bachata 2025", "salsa 2025"
        )

        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<MillaySupabaseSeeder>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
            Log.i(TAG, "Seeder de Supabase encolado.")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Iniciando seeder de Supabase Cerebro Fantasma...")
        var totalSeeded = 0

        // FASE 1: Sembrar música popular de la API (Charts Top 100)
        Log.i(TAG, "FASE 1: Crawleando Charts Populares...")
        for ((chartName, endpoint) in CHART_ENDPOINTS) {
            if (isStopped) break
            try {
                val tracks = fetchPublicChart(endpoint)
                for (track in tracks) {
                    if (isStopped) break
                    if (seedTrack(track)) totalSeeded++
                    delay(200)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error crawleando '$chartName': ${e.message}")
            }
            delay(1000)
        }

        // FASE 2: Sembrar búsquedas editoriales (Nuevas, Recientes, etc.)
        Log.i(TAG, "FASE 2: Crawleando Búsquedas Editoriales...")
        for (query in EDITORIAL_QUERIES) {
            if (isStopped) break
            try {
                val tracks = fetchSearchResults(query)
                for (track in tracks) {
                    if (isStopped) break
                    if (seedTrack(track)) totalSeeded++
                    delay(200)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error en busqueda '$query': ${e.message}")
            }
            delay(1000)
        }

        // FASE 3: Sembrar la librería de música local del usuario
        try {
            Log.i(TAG, "FASE 3: Crawleando librería local...")
            val localSongs = code.name.monkey.retromusic.repository.RealSongRepository(applicationContext).songs()
            val chunkedSongs = localSongs.chunked(100)
            
            for (batch in chunkedSongs) {
                if (isStopped) break
                for (song in batch) {
                    if (isStopped) break
                    val trackId = BpmScanner.generateTrackId(song.artistName, song.title, -1)
                    val existing = SupabaseClientManager.fetchMetadata(trackId)
                    
                    if (existing == null || existing.bpm == 0f) {
                        // Buscar detalles usando busqueda de Deezer para obtener metadata real
                        val searchResults = fetchSearchResults("${song.artistName} ${song.title}", 1)
                        if (searchResults.isNotEmpty()) {
                            val track = searchResults[0]
                            if (seedTrack(track)) totalSeeded++
                        }
                    }
                }
                delay(1000) // Pausa entre lotes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error crawleando local: ${e.message}")
        }



        Log.i(TAG, "Seeder completado: $totalSeeded pistas insertadas en Supabase")
        Result.success()
    }

    private suspend fun seedTrack(track: PublicDeezerTrack): Boolean {
        return try {
            val trackId = BpmScanner.generateTrackId(track.artistName, track.title, track.id)
            val existing = SupabaseClientManager.fetchMetadata(trackId)
            if (existing != null && existing.bpm > 0f) return true

            val detail = fetchTrackDetail(track.id)
            val bpm = detail?.bpmFromApi ?: 0f
            val gain = detail?.gainFromApi ?: 0f
            val genre = detail?.genre ?: ""
            val durationMs = track.durationSec * 1000L

            val fadeMs: Long = when {
                genre.lowercase().contains("reggaet") || genre.lowercase().contains("pop") ||
                genre.lowercase().contains("dance") || genre.lowercase().contains("k-pop") -> 8000L
                genre.lowercase().contains("salsa") || genre.lowercase().contains("bachata") ||
                genre.lowercase().contains("latin") -> 4500L
                else -> 6500L
            }
            val cueOutMs = (durationMs - fadeMs).coerceAtLeast(0L)

            SupabaseClientManager.uploadMetadata(RemoteTrackMetadata(
                trackId = trackId, title = track.title.trim(), artist = track.artistName.trim(),
                bpm = bpm, musicalKey = "", cueOutMs = cueOutMs, replayGain = gain
            ))
            Log.d(TAG, "Seeded: ${track.artistName} - ${track.title} (BPM=$bpm)")
            true
        } catch (e: Exception) { false }
    }

    private fun fetchPublicChart(endpoint: String): List<PublicDeezerTrack> {
        return try {
            val conn = (URL("https://api.deezer.com/$endpoint").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000; readTimeout = 10000; requestMethod = "GET"
                setRequestProperty("User-Agent", "RetroMusic-Milla/1.0")
            }
            if (conn.responseCode != 200) return emptyList()
            val data = JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("data") ?: return emptyList()
            (0 until data.length()).mapNotNull { i ->
                val item = data.getJSONObject(i)
                PublicDeezerTrack(
                    id = item.optLong("id"),
                    title = item.optString("title"),
                    artistName = item.optJSONObject("artist")?.optString("name") ?: "Unknown",
                    durationSec = item.optInt("duration", 0)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchSearchResults(query: String, limit: Int = 50): List<PublicDeezerTrack> {
        return try {
            val encodedQ = URLEncoder.encode(query.trim(), "UTF-8")
            val conn = (URL("https://api.deezer.com/search?q=$encodedQ&limit=$limit").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000; readTimeout = 10000; requestMethod = "GET"
                setRequestProperty("User-Agent", "RetroMusic-Milla/1.0")
            }
            if (conn.responseCode != 200) return emptyList()
            val data = JSONObject(conn.inputStream.bufferedReader().readText()).optJSONArray("data") ?: return emptyList()
            (0 until data.length()).mapNotNull { i ->
                val item = data.getJSONObject(i)
                PublicDeezerTrack(
                    id = item.optLong("id"),
                    title = item.optString("title"),
                    artistName = item.optJSONObject("artist")?.optString("name") ?: "Unknown",
                    durationSec = item.optInt("duration", 0)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchTrackDetail(trackId: Long): TrackDetail? {
        return try {
            val conn = (URL("https://api.deezer.com/track/$trackId").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000; readTimeout = 5000; requestMethod = "GET"
                setRequestProperty("User-Agent", "RetroMusic-Milla/1.0")
            }
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            TrackDetail(
                bpmFromApi = json.optDouble("bpm", 0.0).toFloat(),
                gainFromApi = json.optDouble("gain", 0.0).toFloat(),
                isrc = json.optString("isrc", ""),
                genre = json.optJSONObject("genres")?.optJSONArray("data")?.let { arr ->
                    if (arr.length() > 0) arr.getJSONObject(0).optString("name", "") else ""
                } ?: ""
            )
        } catch (e: Exception) { null }
    }

    data class PublicDeezerTrack(val id: Long, val title: String, val artistName: String, val durationSec: Int)
    data class TrackDetail(val bpmFromApi: Float, val gainFromApi: Float, val isrc: String, val genre: String)
}
