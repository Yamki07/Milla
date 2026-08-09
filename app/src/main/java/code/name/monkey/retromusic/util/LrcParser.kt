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
    val text: String,
    val syllables: List<Syllable> = emptyList()
)

/**
 * API nativa y offline para parsear archivos .lrc físicos o en String.
 * Inspirado en YouLyPlus para la extracción robusta de timestamps y líneas sin depender de conexión a internet.
 */
object LrcParser {

    // RegEx para detectar etiquetas de tiempo de la línea completa [mm:ss.xx]
    private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
    // RegEx para detectar etiquetas de tiempo por sílaba <mm:ss.xx>
    private val SYLLABLE_TIME_PATTERN = Pattern.compile("<(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?>([^<]*)")

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

    /**
     * Genera marcas de tiempo simuladas para letras estáticas basadas en la duración de la canción.
     */
    fun generateEstimatedTimestamps(lines: List<LyricLine>, durationMs: Long): List<LyricLine> {
        if (lines.isEmpty() || durationMs <= 0) return lines
        
        val validLines = lines.filter { it.text.isNotBlank() }
        if (validLines.isEmpty()) return lines

        // Empezamos a iluminar después de un 5% de la canción y terminamos en el 95%
        val startOffset = (durationMs * 0.05).toLong()
        val endOffset = (durationMs * 0.95).toLong()
        val totalLyricDuration = endOffset - startOffset
        
        val timePerLine = totalLyricDuration / validLines.size
        
        val result = mutableListOf<LyricLine>()
        var index = 0
        for (line in lines) {
            if (line.text.isNotBlank()) {
                val simulatedTime = startOffset + (index * timePerLine)
                
                // Generar pseudosílabas para la letra estática para que funcione el efecto ola
                val pseudoSyllables = generatePseudoSyllables(line.text, simulatedTime, timePerLine)
                
                result.add(line.copy(timeMs = simulatedTime, syllables = pseudoSyllables))
                index++
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun generatePseudoSyllables(text: String, startMs: Long, durationMs: Long): List<Syllable> {
        val syllables = mutableListOf<Syllable>()
        // Dividir por espacios para pseudo sílabas
        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
        if (words.isEmpty()) return syllables
        
        val timePerWord = kotlin.math.min(durationMs / words.size, 400L)
        var currentMs = startMs
        for (word in words) {
            syllables.add(Syllable(word, currentMs, timePerWord))
            currentMs += timePerWord
        }
        return syllables
    }

    suspend fun parseSuspending(content: String): List<LyricLine> = withContext(Dispatchers.IO) {
        parse(content)
    }

    private fun parseTimeToMs(minStr: String?, secStr: String?, millisRaw: String?): Long {
        val minutes = minStr?.toLongOrNull() ?: 0L
        val seconds = secStr?.toLongOrNull() ?: 0L
        val millis = when (millisRaw?.length) {
            1 -> (millisRaw.toLongOrNull() ?: 0L) * 100L
            2 -> (millisRaw.toLongOrNull() ?: 0L) * 10L
            3 -> millisRaw.toLongOrNull() ?: 0L
            else -> 0L
        }
        return minutes * 60_000L + seconds * 1_000L + millis
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
                val timeMs = parseTimeToMs(matcher.group(1), matcher.group(2), matcher.group(3))
                timestamps.add(timeMs)
                lastEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = if (lastEnd < line.length) line.substring(lastEnd).trim() else ""
                
                // Extraer sílabas si existen <00:00.00>word
                val syllables = mutableListOf<Syllable>()
                val syllableMatcher = SYLLABLE_TIME_PATTERN.matcher(text)
                
                var cleanText = ""
                while (syllableMatcher.find()) {
                    val sylTime = parseTimeToMs(syllableMatcher.group(1), syllableMatcher.group(2), syllableMatcher.group(3))
                    val sylText = syllableMatcher.group(4) ?: ""
                    cleanText += sylText
                    syllables.add(Syllable(sylText, sylTime, 0L))
                }

                // Calcular la duración de cada sílaba en base a la siguiente
                if (syllables.isNotEmpty()) {
                    for (i in 0 until syllables.size - 1) {
                        val duration = syllables[i + 1].startMs - syllables[i].startMs
                        syllables[i] = syllables[i].copy(durationMs = if (duration > 0) duration else 100L)
                    }
                    // Última sílaba: asumimos una duración corta estándar o prolongada si es el final de la línea
                    if (syllables.isNotEmpty()) {
                        syllables[syllables.size - 1] = syllables.last().copy(durationMs = 800L)
                    }
                } else {
                    cleanText = text // No hay sílabas
                }

                for (time in timestamps) {
                    result.add(LyricLine(time, cleanText.trim(), syllables.toList()))
                }
            }
        }

        // Post-procesamiento: ajustar duraciones de la última sílaba con respecto a la siguiente línea
        // Y generar sílabas simuladas para letras que no tienen sincronización por palabra
        val sortedResult = result.sortedBy { it.timeMs }.toMutableList()
        for (i in 0 until sortedResult.size - 1) {
            val currentLine = sortedResult[i]
            val nextLine = sortedResult[i + 1]
            if (currentLine.syllables.isNotEmpty()) {
                val lastSyl = currentLine.syllables.last()
                var newDuration = nextLine.timeMs - lastSyl.startMs
                if (newDuration > 5000L) newDuration = 5000L // Cap at 5s
                if (newDuration < 100L) newDuration = 500L
                val modifiedSyllables = currentLine.syllables.toMutableList()
                modifiedSyllables[modifiedSyllables.size - 1] = lastSyl.copy(durationMs = newDuration)
                sortedResult[i] = currentLine.copy(syllables = modifiedSyllables)
            } else if (currentLine.text.isNotBlank()) {
                // Generar sílabas simuladas (palabra por palabra)
                var lineDuration = nextLine.timeMs - currentLine.timeMs
                if (lineDuration <= 0L) lineDuration = 2000L
                if (lineDuration > 10000L) lineDuration = 10000L // Cap maximum line duration to 10s
                
                val words = currentLine.text.split(" ")
                val durationPerWord = lineDuration / words.size
                
                val newSyllables = mutableListOf<Syllable>()
                var currentMs = currentLine.timeMs
                for (j in words.indices) {
                    val wordText = words[j] + if (j < words.size - 1) " " else ""
                    newSyllables.add(Syllable(wordText, currentMs, durationPerWord))
                    currentMs += durationPerWord
                }
                sortedResult[i] = currentLine.copy(syllables = newSyllables)
            }
        }
        
        // Manejar la última línea si no tiene sílabas
        if (sortedResult.isNotEmpty()) {
            val lastLineIndex = sortedResult.size - 1
            val lastLine = sortedResult[lastLineIndex]
            if (lastLine.syllables.isEmpty() && lastLine.text.isNotBlank()) {
                val words = lastLine.text.split(" ")
                val durationPerWord = 3000L / words.size // Asumimos 3 segundos en total
                val newSyllables = mutableListOf<Syllable>()
                var currentMs = lastLine.timeMs
                for (j in words.indices) {
                    val wordText = words[j] + if (j < words.size - 1) " " else ""
                    newSyllables.add(Syllable(wordText, currentMs, durationPerWord))
                    currentMs += durationPerWord
                }
                sortedResult[lastLineIndex] = lastLine.copy(syllables = newSyllables)
            }
        }

        return sortedResult
    }
}
