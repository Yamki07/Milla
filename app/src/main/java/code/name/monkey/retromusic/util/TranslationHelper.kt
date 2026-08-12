package code.name.monkey.retromusic.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TranslationHelper {
    suspend fun translateLyrics(lines: List<LyricLine>, targetLang: String = "es"): List<LyricLine> {
        return withContext(Dispatchers.IO) {
            val translatedLines = mutableListOf<LyricLine>()
            
            // Milla AutoMix: Translate each line asynchronously using Coroutines for perfect alignment
            // Eliminamos el batching que rompe los saltos de linea y desincroniza.
            val deferredTranslations = kotlinx.coroutines.coroutineScope {
                lines.map { originalLine ->
                    async {
                        if (originalLine.text.isBlank()) return@async originalLine
                    try {
                        val encodedText = URLEncoder.encode(originalLine.text, "UTF-8")
                        // Usamos un endpoint público confiable de traductor
                        val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        
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
                        
                        val translatedPart = translatedTextSb.toString().trim()
                        
                        // Agregar traducción debajo de la original si es distinta
                        val combinedText = if (translatedPart.isNotEmpty() && !originalLine.text.contains(translatedPart)) {
                            "${originalLine.text}\n$translatedPart"
                        } else {
                            originalLine.text
                        }
                        
                        originalLine.copy(text = combinedText)
                    } catch (e: Exception) {
                        Log.e("TranslationHelper", "Error translating line: ${originalLine.text}", e)
                        originalLine
                    }
                }
            }
            }
            
            // Esperar todas las traducciones en paralelo (rapidísimo)
            translatedLines.addAll(kotlinx.coroutines.awaitAll(*deferredTranslations.toTypedArray()))
            
            translatedLines
        }
    }
}
