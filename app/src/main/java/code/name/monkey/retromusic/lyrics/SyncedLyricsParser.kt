package code.name.monkey.retromusic.lyrics

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Línea mínima, inmutable y ordenable consumida por la vista de letras sincronizadas. */
data class SyncedLyricLine(val startTimeMs: Long, val text: String)

/**
 * Parser tolerante para LRC y cargas JSON de Supabase. No inventa timestamps para letras planas:
 * si no hay tiempos verificables, devuelve una lista vacía y la interfaz informa el estado.
 */
object SyncedLyricsParser {
    private val lrcLine = Regex("""((?:\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?])+)(.*)""")
    private val lrcTime = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val yrcLine = Regex("""\[(\d{1,8}),\d{1,8}](.*)""")
    private val inlineTiming = Regex("""<(?:\d{1,3}:\d{2}(?:[.:]\d{1,3})?|\d{1,8}(?:,\d{1,8})?)>|\(\d{1,8},\d{1,8}(?:,\d{1,8})?\)""")
    private val whitespace = Regex("\\s+")

    fun parse(source: String?): List<SyncedLyricLine> {
        val normalized = source.orEmpty().removePrefix("\uFEFF").trim()
        if (normalized.isBlank()) return emptyList()
        return if (normalized.startsWith("{") || normalized.startsWith("[")) parseJson(normalized) else parseLrc(normalized)
    }

    fun currentLineIndex(lines: List<SyncedLyricLine>, positionMs: Long, leadMs: Long = 150L): Int {
        if (lines.isEmpty()) return -1
        val target = (positionMs + leadMs).coerceAtLeast(0L)
        var low = 0
        var high = lines.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lines[middle].startTimeMs <= target) low = middle + 1 else high = middle - 1
        }
        return high.coerceIn(0, lines.lastIndex)
    }

    private fun parseLrc(source: String): List<SyncedLyricLine> {
        val result = mutableListOf<SyncedLyricLine>()
        var offsetMs = 0L
        source.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("[offset:", true)) {
                offsetMs = line.substringAfter(':').substringBefore(']').trim().toLongOrNull() ?: 0L
                return@forEach
            }
            val yrc = yrcLine.matchEntire(line)
            if (yrc != null) {
                yrc.groupValues[1].toLongOrNull()?.let { time -> result += SyncedLyricLine(time + offsetMs, clean(yrc.groupValues[2])) }
                return@forEach
            }
            val match = lrcLine.matchEntire(line) ?: return@forEach
            val text = clean(match.groupValues[2])
            lrcTime.findAll(match.groupValues[1]).forEach { token ->
                result += SyncedLyricLine(toMs(token.groupValues[1], token.groupValues[2], token.groupValues[3]) + offsetMs, text)
            }
        }
        return normalize(result)
    }

    private fun parseJson(source: String): List<SyncedLyricLine> = runCatching {
        val root = if (source.trimStart().startsWith("[")) JSONArray(source) else JSONObject(source)
        val items = when (root) {
            is JSONArray -> root
            is JSONObject -> {
                root.optString("synced_lyrics").takeIf { it.isNotBlank() }?.let { return parse(it) }
                root.optString("lyrics").takeIf { it.isNotBlank() && !it.trimStart().startsWith("[") }?.let { return parse(it) }
                sequenceOf("lines", "lyrics", "syncedLyrics", "data").mapNotNull { key -> root.optJSONArray(key) }.firstOrNull()
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        buildList {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                val time = sequenceOf("startTimeMs", "start_time_ms", "timeMs", "timestamp", "start", "time")
                    .mapNotNull { key -> entry.optLongOrNull(key) }.firstOrNull() ?: continue
                val text = sequenceOf("text", "lyric", "lyrics", "content", "line").map { entry.optString(it) }.firstOrNull { it.isNotBlank() }
                    ?: continue
                add(SyncedLyricLine(time, clean(text)))
            }
        }.let(::normalize)
    }.getOrElse { emptyList() }

    private fun JSONObject.optLongOrNull(key: String): Long? = when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun toMs(minutes: String, seconds: String, fraction: String): Long {
        val millis = when (fraction.length) { 1 -> fraction.toLongOrNull()?.times(100); 2 -> fraction.toLongOrNull()?.times(10); else -> fraction.toLongOrNull() } ?: 0L
        return (minutes.toLongOrNull() ?: 0L) * 60_000L + (seconds.toLongOrNull() ?: 0L) * 1_000L + millis
    }

    private fun clean(value: String): String = value.replace(inlineTiming, "").replace(whitespace, " ").trim()
    private fun normalize(lines: List<SyncedLyricLine>): List<SyncedLyricLine> = lines
        .filter { it.startTimeMs >= 0L && it.text.isNotBlank() }
        .sortedBy { it.startTimeMs }
        .fold(linkedMapOf<Long, String>()) { merged, line ->
            merged[line.startTimeMs] = listOfNotNull(merged[line.startTimeMs], line.text).distinct().joinToString("\n")
            merged
        }.map { (time, text) -> SyncedLyricLine(time, text) }
}
