/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.util

import java.io.File
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Representa una línea de letra sincronizada en el tiempo en milisegundos.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * API nativa y offline para parsear archivos .lrc físicos o en String.
 * Inspirado en YouLyPlus para la extracción robusta de timestamps y líneas sin depender de conexión a internet.
 */
object LrcParser {

    // RegEx para detectar etiquetas de tiempo [mm:ss.xx] o [mm:ss.xxx] o [mm:ss]
    // Ejemplo: [01:23.45] Hello world
    private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")

    /**
     * Parsea un archivo .lrc de forma síncrona/offline.
     */
    fun parse(file: File): List<LyricLine> {
        if (!file.exists() || !file.canRead()) {
            return emptyList()
        }
        return try {
            val content = file.readText(Charsets.UTF_8)
            parse(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parsea un archivo por ruta física String.
     */
    fun parseFile(filePath: String): List<LyricLine> {
        return parse(File(filePath))
    }

    /**
     * Parsea un archivo .lrc de forma asíncrona usando corrutinas (Dispatchers.IO).
     */
    suspend fun parseSuspending(file: File): List<LyricLine> = withContext(Dispatchers.IO) {
        parse(file)
    }

    suspend fun parseSuspending(content: String): List<LyricLine> = withContext(Dispatchers.IO) {
        parse(content)
    }

    /**
     * Parsea una cadena de texto LRC y retorna la lista cronológicamente ordenada de LyricLine.
     */
    fun parse(content: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        if (content.isBlank()) return result

        val lines = content.split("\n", "\r\n", "\r")
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val matcher = LRC_TIME_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()

            var lastEnd = 0
            while (matcher.find()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val millisRaw = matcher.group(3) ?: "0"

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L

                val millis = when (millisRaw.length) {
                    1 -> (millisRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (millisRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> millisRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }

                val timeMs = minutes * 60_000L + seconds * 1_000L + millis
                timestamps.add(timeMs)
                lastEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = if (lastEnd < line.length) {
                    line.substring(lastEnd).trim()
                } else {
                    ""
                }
                for (time in timestamps) {
                    result.add(LyricLine(time, text))
                }
            }
        }

        return result.sortedBy { it.timeMs }
    }
}
