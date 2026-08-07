package code.name.monkey.retromusic.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

object AmllLyricsFetcher {
    private const val TAG = "AmllLyricsFetcher"
    private val client = OkHttpClient()

    suspend fun fetchLyrics(trackName: String, artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$trackName $artistName", "UTF-8")
            val searchUrl = "https://api.amll.dev/v1/lyrics/search?q=$query"
            
            val searchRequest = Request.Builder().url(searchUrl).get().build()
            val searchResponse = client.newCall(searchRequest).execute()
            val searchBody = searchResponse.body?.string() ?: return@withContext null
            
            val searchJson = JSONObject(searchBody)
            val dataArr = searchJson.optJSONArray("data")
            if (dataArr == null || dataArr.length() == 0) return@withContext null
            
            val songId = dataArr.getJSONObject(0).optString("id")
            if (songId.isEmpty()) return@withContext null
            
            val getUrl = "https://api.amll.dev/v1/lyrics/get?id=$songId"
            val getRequest = Request.Builder().url(getUrl).get().build()
            val getResponse = client.newCall(getRequest).execute()
            val getBody = getResponse.body?.string() ?: return@withContext null
            
            val getJson = JSONObject(getBody)
            val ttmlData = getJson.optJSONObject("data")?.optString("lyrics")
            if (ttmlData.isNullOrEmpty()) return@withContext null
            
            return@withContext parseTtmlToLrc(ttmlData)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching AMLL lyrics: $e")
            null
        }
    }

    private fun parseTtmlToLrc(ttml: String): String {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            // Try to ignore namespaces or use them loosely
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val inputStream = ByteArrayInputStream(ttml.toByteArray(Charsets.UTF_8))
            val document = builder.parse(inputStream)
            
            val lrcLines = mutableListOf<String>()
            
            val pList = document.getElementsByTagNameNS("*", "p")
            if (pList.length == 0) {
                // Try without NS if first attempt fails
                val pListFallback = document.getElementsByTagName("p")
                if (pListFallback.length == 0) return ""
            }
            
            val elements = if (pList.length > 0) pList else document.getElementsByTagName("p")
            
            for (i in 0 until elements.length) {
                val pNode = elements.item(i)
                if (pNode.nodeType == Node.ELEMENT_NODE) {
                    val pElement = pNode as Element
                    val beginAttr = pElement.getAttribute("begin").takeIf { it.isNotEmpty() } ?: "0:00"
                    val lineStart = parseTime(beginAttr)
                    val lineTag = formatTimeLrc(lineStart)
                    
                    val spanList = pElement.getElementsByTagNameNS("*", "span")
                    val spans = if (spanList.length > 0) spanList else pElement.getElementsByTagName("span")
                    
                    if (spans.length > 0) {
                        var currentLine = ""
                        var isFirstWord = true
                        
                        for (j in 0 until spans.length) {
                            val spanNode = spans.item(j)
                            if (spanNode.nodeType == Node.ELEMENT_NODE) {
                                val spanElement = spanNode as Element
                                val spanBegin = spanElement.getAttribute("begin").takeIf { it.isNotEmpty() } ?: beginAttr
                                val wordStart = parseTime(spanBegin)
                                val timeTag = "<${formatTimeLrc(wordStart).substring(1, formatTimeLrc(wordStart).length - 1)}>"
                                
                                val text = spanElement.textContent ?: ""
                                
                                if (isFirstWord) {
                                    currentLine += "$lineTag$timeTag$text"
                                    isFirstWord = false
                                } else {
                                    currentLine += "$timeTag$text"
                                }
                            }
                        }
                        if (currentLine.isNotEmpty()) {
                            lrcLines.add(currentLine)
                        }
                    } else {
                        // Fallback si no hay spans, solo la línea entera
                        val text = pElement.textContent ?: ""
                        if (text.isNotEmpty()) {
                            lrcLines.add("$lineTag$text")
                        }
                    }
                }
            }
            
            return lrcLines.joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing TTML: $e")
            return ""
        }
    }

    private fun parseTime(timeStr: String): Float {
        if (timeStr.isEmpty()) return 0f
        
        try {
            if (timeStr.contains(":")) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    return parts[0].toFloat() * 60f + parts[1].toFloat()
                } else if (parts.size == 3) {
                    return parts[0].toFloat() * 3600f + parts[1].toFloat() * 60f + parts[2].toFloat()
                }
            } else if (timeStr.endsWith("s")) {
                return timeStr.replace("s", "").toFloat()
            } else {
                return timeStr.toFloat()
            }
        } catch (e: Exception) {
            return 0f
        }
        return 0f
    }

    private fun formatTimeLrc(seconds: Float): String {
        val mins = (seconds / 60).toInt()
        val secs = seconds % 60
        // Formato [00:00.00]
        return String.format("[%02d:%05.2f]", mins, secs).replace(',', '.')
    }
}
