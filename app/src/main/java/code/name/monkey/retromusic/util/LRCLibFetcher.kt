/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.util

import android.net.Uri
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetcher para LRCLIB: obtiene letras sincronizadas o normales de internet.
 */
object LRCLibFetcher {

    /**
     * Busca letras (preferiblemente sincronizadas con sílabas) para la canción dada.
     * Si las encuentra, retorna el string con la letra (formato LRC o Enhanced LRC).
     */
    suspend fun fetchLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        try {
            val trackName = URLEncoder.encode(song.title, "UTF-8")
            val artistName = URLEncoder.encode(song.artistName, "UTF-8")
            val albumName = URLEncoder.encode(song.albumName, "UTF-8")
            val duration = song.duration / 1000 // Segundos

            // Construir URL: https://lrclib.net/api/get?track_name=xxx&artist_name=yyy&album_name=zzz&duration=120
            val urlString = "https://lrclib.net/api/get?track_name=$trackName&artist_name=$artistName&album_name=$albumName&duration=$duration"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Milla Music Player / 1.0 (https://github.com/yamki07/Milla)")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                
                // Priorizar syncedLyrics
                if (json.has("syncedLyrics") && !json.isNull("syncedLyrics")) {
                    val synced = json.getString("syncedLyrics")
                    if (synced.isNotBlank()) return@withContext synced
                }
                
                // Fallback a letras normales
                if (json.has("plainLyrics") && !json.isNull("plainLyrics")) {
                    val plain = json.getString("plainLyrics")
                    if (plain.isNotBlank()) return@withContext plain
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
