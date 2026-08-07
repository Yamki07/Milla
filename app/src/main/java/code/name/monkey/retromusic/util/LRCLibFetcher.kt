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
            // Usar API de búsqueda para mayor compatibilidad
            val query = URLEncoder.encode("${song.title} ${song.artistName}", "UTF-8")
            val urlString = "https://lrclib.net/api/search?q=$query"
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

                val jsonArray = org.json.JSONArray(response.toString())
                if (jsonArray.length() > 0) {
                    // Iterar para encontrar el mejor resultado con syncedLyrics
                    var plainFallback: String? = null
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        if (obj.has("syncedLyrics") && !obj.isNull("syncedLyrics")) {
                            val synced = obj.getString("syncedLyrics")
                            if (synced.isNotBlank()) return@withContext synced
                        }
                        if (plainFallback == null && obj.has("plainLyrics") && !obj.isNull("plainLyrics")) {
                            val plain = obj.getString("plainLyrics")
                            if (plain.isNotBlank()) plainFallback = plain
                        }
                    }
                    if (plainFallback != null) return@withContext plainFallback
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
