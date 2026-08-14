package code.name.monkey.retromusic.automix

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

object SongBpmScraper {
    private const val TAG = "SongBpmScraper"
    private const val BASE_URL = "https://songbpm.com"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    data class ScrapedMetadata(
        val artist: String,
        val title: String,
        val bpm: Float,
        val key: String,
        val durationMs: Long,
        val mood: String? = null,
        val halfTimeBpm: Float? = null,
        val mode: String? = null,
        val energy: String? = null,
        val danceability: String? = null,
        val timeSignature: Int? = null,
        val doubleTimeBpm: Float? = null
    )

    suspend fun search(artist: String, title: String): ScrapedMetadata? = withContext(Dispatchers.IO) {
        try {
            val query = "$artist $title".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL/?q=$encodedQuery"
            
            Log.d(TAG, "Buscando en songbpm: $url")
            
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            // Buscar la primera tarjeta de resultado
            val resultCard = doc.selectFirst("div.bg-card > a") ?: return@withContext null

            // Extraer Título y Artista
            val artistElement = resultCard.selectFirst("p.text-sm.uppercase")
            val titleElement = resultCard.selectFirst("p.text-lg")
            
            // Extraer campos clave
            val keyElement = resultCard.select("div.flex-col:has(span:contains(Key)) > span:not(:contains(Key))").first()
            val durationElement = resultCard.select("div.flex-col:has(span:contains(Duration)) > span:not(:contains(Duration))").first()
            val bpmElement = resultCard.select("div.flex-col:has(span:contains(BPM)) > span:not(:contains(BPM))").first()

            val scrapedArtist = artistElement?.text()?.trim() ?: ""
            val scrapedTitle = titleElement?.text()?.trim() ?: ""
            val scrapedKey = keyElement?.text()?.trim() ?: ""
            val scrapedDurationStr = durationElement?.text()?.trim() ?: "0:00"
            val scrapedBpm = bpmElement?.text()?.trim()?.toFloatOrNull() ?: 0f

            val durationMs = parseDuration(scrapedDurationStr)

            if (scrapedBpm > 0f) {
                // Fetch extra details from the specific song page
                val detailHref = resultCard.attr("href")
                val detailUrl = if (detailHref.startsWith("/")) "$BASE_URL$detailHref" else detailHref
                
                var mood: String? = null
                var halfTimeBpm: Float? = null
                var mode: String? = null
                var energy: String? = null
                var danceability: String? = null
                var timeSignature: Int? = null
                var doubleTimeBpm: Float? = null
                
                try {
                    val detailDoc = Jsoup.connect(detailUrl)
                        .userAgent(USER_AGENT)
                        .timeout(10000)
                        .get()
                        
                    val pElements = detailDoc.select("p")
                    var paragraph = ""
                    for (p in pElements) {
                        val text = p.text()
                        if (text.contains("BPM") && text.contains("song by")) {
                            paragraph = text
                            break
                        }
                    }
                    
                    if (paragraph.isNotEmpty()) {
                        mood = Regex("is a (.*?) song by").find(paragraph)?.groupValues?.getOrNull(1)
                        halfTimeBpm = Regex("half-time at ([\\d.]+) BPM").find(paragraph)?.groupValues?.getOrNull(1)?.toFloatOrNull()
                        mode = Regex("and a (major|minor) mode").find(paragraph)?.groupValues?.getOrNull(1)
                        energy = Regex("It has (.*?) and is").find(paragraph)?.groupValues?.getOrNull(1)
                        danceability = Regex("and is (.*?) with a time").find(paragraph)?.groupValues?.getOrNull(1)
                        timeSignature = Regex("time signature of (\\d+) beats").find(paragraph)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        doubleTimeBpm = Regex("double-time at ([\\d.]+) BPM").find(paragraph)?.groupValues?.getOrNull(1)?.toFloatOrNull()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching detail page: ${e.message}")
                }

                Log.d(TAG, "Encontrado en songbpm: $scrapedTitle por $scrapedArtist (BPM: $scrapedBpm, Key: $scrapedKey, Mood: $mood, Mode: $mode)")
                return@withContext ScrapedMetadata(
                    artist = scrapedArtist,
                    title = scrapedTitle,
                    bpm = scrapedBpm,
                    key = scrapedKey,
                    durationMs = durationMs,
                    mood = mood,
                    halfTimeBpm = halfTimeBpm,
                    mode = mode,
                    energy = energy,
                    danceability = danceability,
                    timeSignature = timeSignature,
                    doubleTimeBpm = doubleTimeBpm
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scrapeando songbpm.com: ${e.message}")
        }
        return@withContext null
    }

    private fun parseDuration(durationStr: String): Long {
        try {
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                val mins = parts[0].toLong()
                val secs = parts[1].toLong()
                return (mins * 60 + secs) * 1000L
            } else if (parts.size == 3) { // horas:minutos:segundos
                val hours = parts[0].toLong()
                val mins = parts[1].toLong()
                val secs = parts[2].toLong()
                return (hours * 3600 + mins * 60 + secs) * 1000L
            }
        } catch (e: Exception) {
            // Ignorar
        }
        return 0L
    }
}
