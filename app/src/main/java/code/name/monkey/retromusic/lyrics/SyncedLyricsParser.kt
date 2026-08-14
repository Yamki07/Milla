package code.name.monkey.retromusic.lyrics

import org.json.JSONArray
import org.json.JSONObject

/** Palabra o sílaba con intervalo absoluto para el modo karaoke. */
data class SyncedLyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)

/** Línea inmutable que conserva tiempos por palabra cuando la fuente los proporciona. */
data class SyncedLyricLine(
    val startTimeMs: Long,
    val text: String,
    val words: List<SyncedLyricWord> = emptyList(),
)

/**
 * Lee LRC convencional, Enhanced LRC/YRC, TTML y cargas JSON de Supabase.
 * Los tiempos no verificables nunca se inventan: una letra plana continúa siendo letra plana.
 */
object SyncedLyricsParser {
    private val lrcLine = Regex("""((?:\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?])+)(.*)""")
    private val lrcTime = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val yrcLine = Regex("""\[(\d{1,8}),\d{1,8}](.*)""")
    private val enhancedWordTime = Regex("""<(\d{1,3}:\d{2}(?:[.:]\d{1,3})?)>""")
    private val ttmlParagraph = Regex("""<p\b([^>]*)>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ttmlSpan = Regex("""<span\b([^>]*)>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val timeAttribute = Regex("""\b(?:begin|start|t)\s*=\s*[\"']?([^\"'\s>]+)""", RegexOption.IGNORE_CASE)
    private val endAttribute = Regex("""\b(?:end|dur)\s*=\s*[\"']?([^\"'\s>]+)""", RegexOption.IGNORE_CASE)
    private val inlineTiming = Regex("""<(?:\d{1,3}:\d{2}(?:[.:]\d{1,3})?|\d{1,8}(?:,\d{1,8})?)>|\(\d{1,8},\d{1,8}(?:,\d{1,8})?\)""")
    private val whitespace = Regex("\\s+")

    fun parse(source: String?): List<SyncedLyricLine> {
        val normalized = source.orEmpty().removePrefix("\uFEFF").trim()
        if (normalized.isBlank()) return emptyList()
        return when {
            normalized.startsWith("<tt", true) || normalized.contains("<p", true) -> parseTtml(normalized)
            looksLikeJson(normalized) -> parseJson(normalized)
            else -> parseLrc(normalized)
        }
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

    private fun looksLikeJson(value: String): Boolean =
        value.startsWith("{") || value.startsWith("[{") || value.startsWith("[\"")

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
                yrc.groupValues[1].toLongOrNull()?.let { time ->
                    val words = parseInlineWords(yrc.groupValues[2], offsetMs)
                    result += SyncedLyricLine(time + offsetMs, textFrom(words, yrc.groupValues[2]), words)
                }
                return@forEach
            }
            val match = lrcLine.matchEntire(line) ?: return@forEach
            val words = parseInlineWords(match.groupValues[2], offsetMs)
            val text = textFrom(words, match.groupValues[2])
            lrcTime.findAll(match.groupValues[1]).forEach { token ->
                result += SyncedLyricLine(toMs(token.groupValues[1], token.groupValues[2], token.groupValues[3]) + offsetMs, text, words)
            }
        }
        return normalize(result)
    }

    private fun parseInlineWords(source: String, offsetMs: Long): List<SyncedLyricWord> {
        val matches = enhancedWordTime.findAll(source).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val start = toClockMs(match.groupValues[1]) + offsetMs
            val textStart = match.range.last + 1
            val textEnd = matches.getOrNull(index + 1)?.range?.first ?: source.length
            val word = clean(source.substring(textStart, textEnd))
            word.takeIf { it.isNotBlank() }?.let { SyncedLyricWord(start, start + DEFAULT_WORD_DURATION_MS, it) }
        }
    }

    private fun parseTtml(source: String): List<SyncedLyricLine> {
        val result = mutableListOf<SyncedLyricLine>()
        ttmlParagraph.findAll(source).forEach { paragraph ->
            val attributes = paragraph.groupValues[1]
            val start = timeAttribute.find(attributes)?.groupValues?.get(1)?.let(::toClockMs) ?: return@forEach
            val provisionalEnd = endAttribute.find(attributes)?.groupValues?.get(1)?.let(::toClockMs)
            val rawBody = paragraph.groupValues[2]
            val words = ttmlSpan.findAll(rawBody).mapNotNull { span ->
                val wordStart = timeAttribute.find(span.groupValues[1])?.groupValues?.get(1)?.let(::toClockMs) ?: return@mapNotNull null
                val wordEnd = endAttribute.find(span.groupValues[1])?.groupValues?.get(1)?.let(::toClockMs) ?: wordStart + DEFAULT_WORD_DURATION_MS
                clean(stripXml(span.groupValues[2])).takeIf { it.isNotBlank() }?.let { SyncedLyricWord(wordStart, wordEnd, it) }
            }.toList()
            val text = if (words.isEmpty()) clean(stripXml(rawBody)) else words.joinToString(" ") { it.text }
            if (text.isNotBlank()) result += SyncedLyricLine(start, text, completeWordEnds(words, provisionalEnd ?: start + DEFAULT_LINE_DURATION_MS))
        }
        return normalize(result)
    }

    private fun parseJson(source: String): List<SyncedLyricLine> = runCatching {
        val root = if (source.trimStart().startsWith("[")) JSONArray(source) else JSONObject(source)
        val items = when (root) {
            is JSONArray -> root
            is JSONObject -> {
                root.optString("synced_lyrics").takeIf { it.isNotBlank() }?.let { return parse(it) }
                root.optString("lyrics").takeIf { it.isNotBlank() && !looksLikeJson(it) }?.let { return parse(it) }
                sequenceOf("lines", "lyrics", "syncedLyrics", "data").mapNotNull { root.optJSONArray(it) }.firstOrNull() ?: return emptyList()
            }
            else -> return emptyList()
        }
        buildList {
            for (index in 0 until items.length()) {
                val entry = items.optJSONObject(index) ?: continue
                val time = sequenceOf("startTimeMs", "start_time_ms", "timeMs", "timestamp", "start", "time")
                    .mapNotNull { entry.optLongOrNull(it) }.firstOrNull() ?: continue
                val text = sequenceOf("text", "lyric", "lyrics", "content", "line").map { entry.optString(it) }.firstOrNull { it.isNotBlank() } ?: continue
                val words = entry.optJSONArray("words") ?: entry.optJSONArray("syllables")
                add(SyncedLyricLine(time, clean(text), parseJsonWords(words)))
            }
        }.let(::normalize)
    }.getOrElse { emptyList() }

    private fun parseJsonWords(items: JSONArray?): List<SyncedLyricWord> = buildList {
        if (items == null) return@buildList
        for (index in 0 until items.length()) {
            val word = items.optJSONObject(index) ?: continue
            val start = sequenceOf("startTimeMs", "start_time_ms", "start", "time").mapNotNull { word.optLongOrNull(it) }.firstOrNull() ?: continue
            val end = sequenceOf("endTimeMs", "end_time_ms", "end").mapNotNull { word.optLongOrNull(it) }.firstOrNull() ?: start + DEFAULT_WORD_DURATION_MS
            val text = sequenceOf("text", "word", "content", "syllable").map { word.optString(it) }.firstOrNull { it.isNotBlank() } ?: continue
            add(SyncedLyricWord(start, end, clean(text)))
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? = when (val value = opt(key)) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun normalize(lines: List<SyncedLyricLine>): List<SyncedLyricLine> {
        val merged = lines.filter { it.startTimeMs >= 0L && it.text.isNotBlank() }
            .sortedBy { it.startTimeMs }
            .fold(linkedMapOf<Long, SyncedLyricLine>()) { map, line ->
                map[line.startTimeMs] = map[line.startTimeMs]?.copy(
                    text = listOf(map[line.startTimeMs]!!.text, line.text).distinct().joinToString("\n"),
                    words = (map[line.startTimeMs]!!.words + line.words).sortedBy { it.startTimeMs },
                ) ?: line
                map
            }.values.toList()
        return merged.mapIndexed { index, line ->
            val end = merged.getOrNull(index + 1)?.startTimeMs ?: line.startTimeMs + DEFAULT_LINE_DURATION_MS
            line.copy(words = completeWordEnds(line.words, end))
        }
    }

    private fun completeWordEnds(words: List<SyncedLyricWord>, lineEndMs: Long): List<SyncedLyricWord> =
        words.sortedBy { it.startTimeMs }.mapIndexed { index, word ->
            val next = words.sortedBy { it.startTimeMs }.getOrNull(index + 1)?.startTimeMs ?: lineEndMs
            word.copy(endTimeMs = word.endTimeMs.coerceAtMost(next).coerceAtLeast(word.startTimeMs + 1L))
        }

    private fun textFrom(words: List<SyncedLyricWord>, source: String): String =
        words.takeIf { it.isNotEmpty() }?.joinToString(" ") { it.text } ?: clean(source)

    private fun stripXml(value: String): String = value.replace(Regex("<[^>]+>"), " ")
    private fun clean(value: String): String = value.replace(inlineTiming, "").replace(whitespace, " ").trim()
    private fun toMs(minutes: String, seconds: String, fraction: String): Long =
        (minutes.toLongOrNull() ?: 0L) * 60_000L + (seconds.toLongOrNull() ?: 0L) * 1_000L + fractionToMs(fraction)
    private fun toClockMs(value: String): Long = when {
        value.endsWith("ms") -> value.removeSuffix("ms").toLongOrNull() ?: 0L
        value.endsWith("s") -> ((value.removeSuffix("s").toDoubleOrNull() ?: 0.0) * 1_000).toLong()
        value.count { it == ':' } == 2 -> {
            val parts = value.split(':'); (parts[0].toLongOrNull() ?: 0L) * 3_600_000L + (parts[1].toLongOrNull() ?: 0L) * 60_000L + ((parts[2].toDoubleOrNull() ?: 0.0) * 1_000).toLong()
        }
        else -> {
            val parts = value.split(':'); if (parts.size == 2) (parts[0].toLongOrNull() ?: 0L) * 60_000L + ((parts[1].toDoubleOrNull() ?: 0.0) * 1_000).toLong() else 0L
        }
    }
    private fun fractionToMs(fraction: String): Long = when (fraction.length) { 1 -> fraction.toLongOrNull()?.times(100); 2 -> fraction.toLongOrNull()?.times(10); else -> fraction.toLongOrNull() } ?: 0L

    private const val DEFAULT_WORD_DURATION_MS = 500L
    private const val DEFAULT_LINE_DURATION_MS = 2_000L
}
