package code.name.monkey.retromusic.util

import android.content.Context
import android.util.Log
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TranslationHelper {
    private const val SUPABASE_EDGE_FUNCTION_URL = "https://brgwlyixvgdvzahmsusf.supabase.co/functions/v1/translate-lyrics"
    private const val SUPABASE_ANON_KEY = "sb_publishable_4qGbvRV8ArCt3OkFe4mcCQ_r9DpCKM1"

    suspend fun translateLyrics(context: Context, song: Song, lines: List<LyricLine>, targetLang: String = "es"): List<LyricLine> {
        return withContext(Dispatchers.IO) {
            if (lines.isEmpty()) return@withContext emptyList()

            // Generate universal track_id
            val trackId = code.name.monkey.retromusic.automix.BpmScanner.generateTrackId(song.artistName, song.title, song.id)

            val supabaseMeta = code.name.monkey.retromusic.network.SupabaseClientManager.fetchMetadata(trackId)
            if (supabaseMeta?.syncedLyricsTranslated != null) {
                return@withContext LrcParser.parse(supabaseMeta.syncedLyricsTranslated)
            }
            
            // Reconstruir LRC original para traducir si es necesario
            val lrcStringBuilder = java.lang.StringBuilder()
            for (line in lines) {
                val minutes = line.timeMs / 60000
                val seconds = (line.timeMs % 60000) / 1000
                val hundredths = (line.timeMs % 1000) / 10
                lrcStringBuilder.append(String.format("[%02d:%02d.%02d]%s\n", minutes, seconds, hundredths, line.text))
            }
            val rawLyrics = lrcStringBuilder.toString()

            // 3. Request translation via Gemini Proxy (Supabase Edge Function)
            try {
                val url = java.net.URL(SUPABASE_EDGE_FUNCTION_URL)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                val jsonPayload = org.json.JSONObject()
                jsonPayload.put("lyrics", rawLyrics)
                jsonPayload.put("targetLanguage", targetLang)

                java.io.OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonPayload.toString())
                    writer.flush()
                }

                if (connection.responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = org.json.JSONObject(responseText)
                    
                    if (jsonResponse.optBoolean("success", false)) {
                        val translatedLyrics = jsonResponse.optString("translatedLyrics", "")
                        
                        // Save to Supabase (global cache)
                        val updatedMeta = code.name.monkey.retromusic.network.RemoteTrackMetadata(
                            trackId = trackId, title = song.title, artist = song.artistName,
                            bpm = 0f, musicalKey = "", cueOutMs = 0L, replayGain = 0f,
                            syncedLyricsTranslated = translatedLyrics // Requires updating RemoteTrackMetadata!
                        )
                        code.name.monkey.retromusic.network.SupabaseClientManager.uploadMetadata(updatedMeta)
                        
                        // Save to Room (local cache)
                        // Actually, updating the song in Room requires the repository.
                        // We will return the parsed lyrics, and the caller (LyricsFragment) should update the local entity.
                        return@withContext LrcParser.parse(translatedLyrics)
                    }
                }
            } catch (e: Exception) {
                Log.e("TranslationHelper", "Exception translating lyrics", e)
            }
            
            lines // Fallback
        }
    }
}
