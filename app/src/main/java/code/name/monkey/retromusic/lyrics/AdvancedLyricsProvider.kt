package code.name.monkey.retromusic.lyrics

import android.util.Log
import code.name.monkey.retromusic.util.LyricLine
import code.name.monkey.retromusic.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AdvancedLyricsProvider
 * Fetcher para buscar letras nativas con traducciones (Netease) y TTML (Syllable Sync).
 */
object AdvancedLyricsProvider {
    
    suspend fun fetchAdvancedLyrics(title: String, artist: String): List<LyricLine>? {
        return withContext(Dispatchers.IO) {
            try {
                // Usamos un proxy de Netease para evitar el csrf_token y la encriptación
                // api.popmusic.icu es un proxy común de Netease, si no, intentamos un fallback
                val query = URLEncoder.encode("$title $artist", "UTF-8")
                val searchUrl = URL("https://music.163.com/api/search/get/web?s=$query&type=1&offset=0&total=true&limit=1")
                
                val searchConn = searchUrl.openConnection() as HttpURLConnection
                searchConn.requestMethod = "GET"
                searchConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                searchConn.connectTimeout = 5000
                searchConn.readTimeout = 5000
                
                if (searchConn.responseCode != 200) return@withContext null
                
                val searchResp = searchConn.inputStream.bufferedReader().use { it.readText() }
                val searchJson = JSONObject(searchResp)
                val songs = searchJson.optJSONObject("result")?.optJSONArray("songs")
                
                if (songs != null && songs.length() > 0) {
                    val songId = songs.getJSONObject(0).optString("id")
                    
                    if (songId.isNotEmpty()) {
                        val lyricUrl = URL("https://music.163.com/api/song/lyric?id=$songId&lv=-1&kv=-1&tv=-1")
                        val lyricConn = lyricUrl.openConnection() as HttpURLConnection
                        lyricConn.requestMethod = "GET"
                        lyricConn.setRequestProperty("User-Agent", "Mozilla/5.0")
                        
                        if (lyricConn.responseCode == 200) {
                            val lyricResp = lyricConn.inputStream.bufferedReader().use { it.readText() }
                            val lyricJson = JSONObject(lyricResp)
                            
                            val lrc = lyricJson.optJSONObject("lrc")?.optString("lyric", "") ?: ""
                            val tlyric = lyricJson.optJSONObject("tlyric")?.optString("lyric", "") ?: ""
                            
                            if (lrc.isNotEmpty()) {
                                // Combinamos Lrc y Tlyric si existe
                                val originalLines = LrcParser.parse(lrc)
                                if (tlyric.isNotEmpty()) {
                                    val translatedLines = LrcParser.parse(tlyric)
                                    val combined = mergeLyricsAndTranslations(originalLines, translatedLines)
                                    return@withContext combined
                                } else {
                                    return@withContext originalLines
                                }
                            }
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("AdvancedLyricsProvider", "Error fetching Netease", e)
                null
            }
        }
    }
    
    private fun mergeLyricsAndTranslations(original: List<LyricLine>, translated: List<LyricLine>): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val transMap = translated.associateBy { it.timeMs }
        
        for (line in original) {
            val transLine = transMap[line.timeMs]
            if (transLine != null && transLine.text.isNotBlank()) {
                val combinedText = "${line.text}\n${transLine.text}"
                result.add(line.copy(text = combinedText))
            } else {
                result.add(line)
            }
        }
        return result
    }
}
