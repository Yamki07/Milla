package code.name.monkey.retromusic.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TranslationHelper {
    suspend fun translateLyrics(lines: List<LyricLine>, targetLang: String = "es"): List<LyricLine> {
        return withContext(Dispatchers.IO) {
            val translatedLines = mutableListOf<LyricLine>()
            
            val batchSize = 15 // Limitar para no exceder URL length
            for (i in lines.indices step batchSize) {
                val batch = lines.subList(i, minOf(i + batchSize, lines.size))
                
                // Usar un separador que la API suela respetar
                val separator = " \n "
                val sb = java.lang.StringBuilder()
                for (line in batch) {
                    sb.append(line.text).append(separator)
                }
                
                try {
                    val encodedText = URLEncoder.encode(sb.toString(), "UTF-8")
                    // Uso de endpoint más confiable (mismo que extensiones de Chrome)
                    val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val jsonResponse = JSONArray(response)
                    val jsonSentences = jsonResponse.getJSONArray(0)
                    val translatedTextSb = StringBuilder()
                    
                    for (j in 0 until jsonSentences.length()) {
                        val sentenceArray = jsonSentences.optJSONArray(j)
                        if (sentenceArray != null) {
                            translatedTextSb.append(sentenceArray.optString(0, ""))
                        }
                    }
                    
                    val translatedParts = translatedTextSb.toString().split("\n")
                    
                    for (j in batch.indices) {
                        val originalLine = batch[j]
                        val translatedPart = translatedParts.getOrNull(j)?.trim() ?: ""
                        
                        // Si ya tiene una traducción (por ejemplo de AMLL), no duplicarla
                        val combinedText = if (translatedPart.isNotEmpty() && !originalLine.text.contains(translatedPart)) {
                            "${originalLine.text}\n$translatedPart"
                        } else {
                            originalLine.text
                        }
                        
                        translatedLines.add(
                            LyricLine(
                                timeMs = originalLine.timeMs,
                                text = combinedText,
                                syllables = originalLine.syllables
                            )
                        )
                    }
                    
                } catch (e: Exception) {
                    Log.e("TranslationHelper", "Error translating batch", e)
                    translatedLines.addAll(batch) // Fallback al original si falla
                }
            }
            
            translatedLines
        }
    }
}
