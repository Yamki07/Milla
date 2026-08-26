/*
 * SyllableLyricsReader.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Reads the SYLLABLE_LYRICS tag written by AudioMetadataInjector from a local
 * audio file and exposes it as a typed, playback-ready list of SyncedLyric
 * objects — with zero network calls and zero database queries.
 *
 * Supported formats
 * ─────────────────
 *   .flac  → reads the custom `SYLLABLE_LYRICS` VorbisComment field first;
 *            falls back to the standard `LYRICS` VorbisComment field.
 *   .m4a   → reads the `----:com.apple.iTunes:SYLLABLE_LYRICS` free-form atom
 *            first; falls back to the standard `©lyr` atom.
 *   .mp3   → reads the USLT frame with desc="SYLLABLE_LYRICS" first;
 *            falls back to the first available USLT frame.
 *
 * JSON canonical format (written by AudioMetadataInjector):
 *   [{"time": 4210, "text": "Primera línea"}, ...]
 *   time = milliseconds from track start
 *
 * LRC fallback format (written by legacy tagging path):
 *   [00:04.21] Primera línea
 *   Automatically converted to SyncedLyric objects.
 *
 * Usage — one call at playback start:
 *   val lyrics = SyllableLyricsReader.readFromFile(File(song.data))
 *
 * Usage — polling current line during playback:
 *   val lineIndex = SyllableLyricsReader.currentLineIndex(lyrics, player.currentPosition)
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.json.JSONArray
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

/**
 * One lyric line or syllable, timestamped in milliseconds.
 *
 * @param timeMs  Absolute position from the start of the track, in ms.
 * @param text    Lyric line or syllable text to display.
 */
data class SyncedLyric(
    val timeMs: Long,
    val text: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Reader
// ─────────────────────────────────────────────────────────────────────────────

object SyllableLyricsReader {

    private const val TAG = "SyllableLyricsReader"

    private const val VORBIS_SYLLABLE_KEY  = "SYLLABLE_LYRICS"
    private const val M4A_SYLLABLE_ATOM    = "----:com.apple.iTunes:SYLLABLE_LYRICS"
    private const val USLT_SYLLABLE_DESC   = "SYLLABLE_LYRICS"

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Read and parse the SYLLABLE_LYRICS tag from [file] on the IO dispatcher.
     *
     * Returns an empty list (never null) so callers can use it directly without
     * null checks:
     *   `if (lyrics.isEmpty()) showPlaceholder() else startScrolling(lyrics)`
     *
     * @param file  Local audio file (.flac, .m4a, or .mp3).
     */
    suspend fun readFromFile(file: File): List<SyncedLyric> =
        withContext(Dispatchers.IO) {
            if (!file.exists()) {
                Log.w(TAG, "readFromFile: file not found — ${file.path}")
                return@withContext emptyList()
            }
            val raw = readRawLyricsString(file)
            if (raw.isNullOrBlank()) {
                Log.d(TAG, "readFromFile: no lyrics tag in ${file.name}")
                return@withContext emptyList()
            }
            parseToSyncedLyrics(raw).also {
                Log.d(TAG, "readFromFile: parsed ${it.size} lines from ${file.name}")
            }
        }

    /**
     * Synchronous variant — call only from a background thread / coroutine.
     * Prefer [readFromFile] (suspend) when calling from a coroutine context.
     */
    fun readFromFileBlocking(file: File): List<SyncedLyric> {
        if (!file.exists()) return emptyList()
        val raw = readRawLyricsString(file) ?: return emptyList()
        return parseToSyncedLyrics(raw)
    }

    /**
     * Given the current playback position [positionMs], return the index of the
     * active lyric line in [lyrics].
     *
     * Uses binary search for O(log n) performance — safe to call every frame.
     *
     * Returns -1 if [lyrics] is empty or [positionMs] is before the first line.
     */
    fun currentLineIndex(lyrics: List<SyncedLyric>, positionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var lo = 0
        var hi = lyrics.lastIndex
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lyrics[mid].timeMs <= positionMs) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return result
    }

    /**
     * Returns the next lyric line after [positionMs], or null if at/past the end.
     * Useful for pre-animating the upcoming line before it becomes active.
     */
    fun nextLine(lyrics: List<SyncedLyric>, positionMs: Long): SyncedLyric? {
        val idx = currentLineIndex(lyrics, positionMs)
        return lyrics.getOrNull(idx + 1)
    }

    /**
     * Returns the milliseconds until the next lyric line starts, or null if at end.
     * Useful for scheduling highlight animations precisely.
     */
    fun msUntilNextLine(lyrics: List<SyncedLyric>, positionMs: Long): Long? {
        val next = nextLine(lyrics, positionMs) ?: return null
        return (next.timeMs - positionMs).coerceAtLeast(0L)
    }

    // ── Format-specific raw readers ────────────────────────────────────────────

    private fun readRawLyricsString(file: File): String? {
        return try {
            org.jaudiotagger.tag.TagOptionSingleton.getInstance().isAndroid = true
            val af  = AudioFileIO.read(file)
            val tag = af.tag ?: return null

            when (tag) {
                is FlacTag         -> readFlacLyrics(tag)
                is Mp4Tag          -> readM4aLyrics(tag)
                is AbstractID3v2Tag -> readMp3Lyrics(tag)
                else               -> tag.getFirst(FieldKey.LYRICS).takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "readRawLyricsString error for ${file.name}: $e")
            null
        }
    }

    /**
     * FLAC: prefer `SYLLABLE_LYRICS` custom field; fall back to `LYRICS`.
     */
    private fun readFlacLyrics(tag: FlacTag): String? {
        val syllable = runCatching { tag.getFirst(VORBIS_SYLLABLE_KEY) }
            .getOrNull()?.takeIf { it.isNotBlank() }
        if (syllable != null) return syllable

        return runCatching { tag.getFirst(FieldKey.LYRICS) }
            .getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * M4A: prefer `----:com.apple.iTunes:SYLLABLE_LYRICS` atom; fall back to `©lyr`.
     */
    private fun readM4aLyrics(tag: Mp4Tag): String? {
        val syllable = runCatching { tag.getFirst(M4A_SYLLABLE_ATOM) }
            .getOrNull()?.takeIf { it.isNotBlank() }
        if (syllable != null) return syllable

        return runCatching { tag.getFirst(FieldKey.LYRICS) }
            .getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * MP3/ID3v2: prefer USLT with desc=`SYLLABLE_LYRICS`; fall back to first USLT.
     */
    private fun readMp3Lyrics(tag: AbstractID3v2Tag): String? {
        // Walk all USLT frames to find the one with our description
        val usltFields = runCatching { tag.getFields("USLT") }.getOrNull()
        if (usltFields != null) {
            // Priority 1: frame with our custom desc
            val syllableFrame = usltFields
                .mapNotNull { (it as? org.jaudiotagger.tag.id3.AbstractID3v2Frame)?.body as? FrameBodyUSLT }
                .firstOrNull { it.description == USLT_SYLLABLE_DESC }
            if (syllableFrame?.firstTextValue?.isNotBlank() == true) return syllableFrame.firstTextValue

            // Priority 2: any USLT frame
            val fallbackFrame = usltFields
                .mapNotNull { (it as? org.jaudiotagger.tag.id3.AbstractID3v2Frame)?.body as? FrameBodyUSLT }
                .firstOrNull { it.firstTextValue?.isNotBlank() == true }
            if (fallbackFrame?.firstTextValue != null) return fallbackFrame.firstTextValue
        }

        // Last resort: generic FieldKey.LYRICS
        return runCatching { tag.getFirst(FieldKey.LYRICS) }
            .getOrNull()?.takeIf { it.isNotBlank() }
    }

    // ── Parsing ────────────────────────────────────────────────────────────────

    /**
     * Parse [raw] into a list of [SyncedLyric].
     *
     * Tries canonical JSON first; if that fails, tries LRC format.
     * Returns an empty list if neither can be parsed.
     */
    private fun parseToSyncedLyrics(raw: String): List<SyncedLyric> {
        val trimmed = raw.trim()

        // ── Attempt 1: canonical JSON ──────────────────────────────────────────
        if (trimmed.startsWith("[")) {
            val fromJson = runCatching { parseJson(trimmed) }.getOrNull()
            if (!fromJson.isNullOrEmpty()) return fromJson
        }

        // ── Attempt 2: LRC format ──────────────────────────────────────────────
        if (trimmed.contains("[") && trimmed.contains(":")) {
            val fromLrc = parseLrc(trimmed)
            if (fromLrc.isNotEmpty()) return fromLrc
        }

        // ── Attempt 3: plain text (unsynchronized) — single entry at t=0 ───────
        if (trimmed.isNotBlank()) {
            return listOf(SyncedLyric(timeMs = 0L, text = trimmed))
        }

        return emptyList()
    }

    /** Parse canonical `[{"time": ms, "text": "..."}]` JSON. */
    private fun parseJson(json: String): List<SyncedLyric> {
        val arr = JSONArray(json)
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj  = arr.optJSONObject(i) ?: continue
                val time = obj.optLong("time", -1L)
                val text = obj.optString("text", "").trim()
                if (time >= 0L) add(SyncedLyric(timeMs = time, text = text))
            }
        }.sortedBy { it.timeMs }
    }

    /**
     * Parse LRC lines:  `[mm:ss.xx] Lyric text`
     * Also handles `[mm:ss.xxx]` (three decimal places).
     */
    private fun parseLrc(lrc: String): List<SyncedLyric> {
        val regex = Regex("""^\[(\d{1,3}:\d{2}[.:]\d{1,3})]\s*(.*)$""")
        return lrc.lines()
            .mapNotNull { line ->
                val match = regex.matchEntire(line.trim()) ?: return@mapNotNull null
                val ms    = parseLrcTimestamp(match.groupValues[1])
                val text  = match.groupValues[2].trim()
                SyncedLyric(timeMs = ms, text = text)
            }
            .sortedBy { it.timeMs }
    }

    /** Parse `mm:ss.xx`, `mm:ss:xx`, or `mm:ss.xxx` → milliseconds. */
    private fun parseLrcTimestamp(ts: String): Long {
        return try {
            // Normalise separator between seconds and centiseconds/milliseconds
            val normalised = ts.replace(':', '.').let {
                val parts = it.split(".")
                if (parts.size == 3) "${parts[0]}:${parts[1]}.${parts[2]}" else ts
            }
            val colonIdx = normalised.indexOf(':')
            val mm = normalised.substring(0, colonIdx).toLong()
            val ss = normalised.substring(colonIdx + 1).toDouble()
            mm * 60_000L + (ss * 1_000.0).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}
