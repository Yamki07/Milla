package code.name.monkey.retromusic.lyrics

import android.util.Log
import code.name.monkey.retromusic.util.LyricLine
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AiLyricsTranslator
 * Traduce letras línea por línea manteniendo sincronización y contexto poético.
 * Incluye procesamiento por lotes (chunking) para la IA y un fallback a Google Translate gratuito.
 */
object AiLyricsTranslator {
    private const val TAG = "AiLyricsTranslator"
    private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-8b:generateContent"
    private const val MAX_ITEMS_PER_BATCH = 60

    suspend fun translate(lines: List<LyricLine>, targetLang: String = "español"): List<LyricLine> {
        if (lines.isEmpty()) return lines

        // Usamos la API Key desde las preferencias, si está configurada.
        val geminiKey = PreferenceUtil.geminiApiKey

        return withContext(Dispatchers.IO) {
            if (geminiKey.isNullOrBlank() || geminiKey == "PON_TU_API_KEY_AQUI") {
                Log.i(TAG, "No Gemini API Key found. Using Free Google Translate Fallback.")
                return@withContext fallbackTranslate(lines, targetLang)
            }

            Log.i(TAG, "Using Gemini AI for translation.")
            val translatedLines = mutableListOf<LyricLine>()
            
            // Chunking to avoid API hallucinations and timeouts
            val chunks = lines.chunked(MAX_ITEMS_PER_BATCH)
            
            for (chunk in chunks) {
                try {
                    val payloadArray = JSONArray()
                    chunk.forEach { payloadArray.put(it.text) }

                    val systemPrompt = """
                        You are an expert song lyrics translator.
                        Translate each input string into $targetLang with natural, accurate lyric phrasing.
                        Preserve meaning, tone, profanity level, names, repeated hooks, and line-level intent.
                        Do not add timestamps, IDs, XML, markdown, explanations, or extra lines.
                        Return ONLY a JSON array of strings with exactly ${chunk.size} items in the exact same order.
                    """.trimIndent()

                    val userPrompt = payloadArray.toString()

                    val requestBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().put("text", "$systemPrompt\n\n$userPrompt"))
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.15)
                            put("maxOutputTokens", 8192)
                            put("responseMimeType", "application/json")
                        })
                    }.toString()

                    val url = URL(GEMINI_ENDPOINT)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("x-goog-api-key", geminiKey)
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 45000

                    connection.outputStream.use { os ->
                        val input = requestBody.toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonResponse = JSONObject(responseStr)
                        
                        val textContent = jsonResponse
                            .optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")

                        if (textContent != null) {
                            val translatedArray = JSONArray(textContent)
                            if (translatedArray.length() == chunk.size) {
                                for (i in 0 until translatedArray.length()) {
                                    val translatedText = translatedArray.optString(i, "").trim()
                                    val originalLine = chunk[i]
                                    
                                    val combinedText = if (translatedText.isNotEmpty() && !originalLine.text.equals(translatedText, ignoreCase = true)) {
                                        "${originalLine.text}\n$translatedText"
                                    } else {
                                        originalLine.text
                                    }
                                    translatedLines.add(originalLine.copy(text = combinedText))
                                }
                            } else {
                                Log.e(TAG, "Gemini returned ${translatedArray.length()} lines instead of ${chunk.size}. Falling back for this chunk.")
                                translatedLines.addAll(fallbackTranslate(chunk, targetLang))
                            }
                        }
                    } else {
                        val errorResp = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(TAG, "API Error ($responseCode): $errorResp")
                        translatedLines.addAll(fallbackTranslate(chunk, targetLang))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception calling Gemini: ${e.message}")
                    translatedLines.addAll(fallbackTranslate(chunk, targetLang))
                }
            }
            translatedLines
        }
    }

    /**
     * Fallback gratuito usando la API pública de Google Translate (Client GTX)
     * Procesa la letra rápidamente si el usuario no tiene API Key.
     */
    private fun fallbackTranslate(lines: List<LyricLine>, targetLang: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        try {
            val targetLangCode = if (targetLang.lowercase().contains("español")) "es" else "en"
            
            // Enviar texto completo unido por saltos de línea
            val textToTranslate = lines.joinToString("\n") { it.text }
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLangCode&dt=t"
            val url = URL(urlStr)
            
            val payload = "q=" + URLEncoder.encode(textToTranslate, "UTF-8")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            
            connection.outputStream.use { os ->
                val input = payload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseStr)
                val translations = jsonArray.optJSONArray(0)
                
                if (translations != null) {
                    val fullTranslatedText = StringBuilder()
                    for (i in 0 until translations.length()) {
                        fullTranslatedText.append(translations.optJSONArray(i)?.optString(0, "") ?: "")
                    }
                    
                    val translatedLinesArray = fullTranslatedText.toString().split("\n")
                    
                    if (translatedLinesArray.size >= lines.size - 2) {
                        for (i in lines.indices) {
                            val translatedText = translatedLinesArray.getOrNull(i)?.trim() ?: ""
                            val originalLine = lines[i]
                            val combinedText = if (translatedText.isNotEmpty() && !originalLine.text.equals(translatedText, ignoreCase = true)) {
                                "${originalLine.text}\n$translatedText"
                            } else {
                                originalLine.text
                            }
                            result.add(originalLine.copy(text = combinedText))
                        }
                    } else {
                        result.addAll(lines) // Fallback
                    }
                } else {
                    result.addAll(lines)
                }
            } else {
                result.addAll(lines)
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Fallback translation error: ${e.message}")
            return lines
        }
    }
}
