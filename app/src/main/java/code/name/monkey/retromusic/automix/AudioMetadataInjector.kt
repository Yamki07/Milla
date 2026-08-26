/*
 * AudioMetadataInjector.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Audio metadata injection service for Milla — Android / jaudiotagger edition.
 *
 * Embeds into a freshly downloaded .flac or .m4a file:
 *   • Title, Artist, Album
 *   • BPM
 *   • Cover Art (JPEG)
 *   • Syllable-Synced Lyrics JSON
 *
 * The result is a 100 % self-sufficient offline file: no network call or Room
 * query is required at playback time to display artwork, tags, or lyrics.
 *
 * ┌──────────┬────────────────────────────────────────────────────────────────┐
 * │ Format   │ Tags written                                                   │
 * ├──────────┼────────────────────────────────────────────────────────────────┤
 * │ FLAC     │ VorbisComment: TITLE, ARTIST, ALBUM, BPM                       │
 * │          │   LYRICS          — plain JSON (broad player compat)           │
 * │          │   SYLLABLE_LYRICS — Milla custom field (syllable renderer)     │
 * │          │ FLAC Picture block, type 3 (front cover)                       │
 * ├──────────┼────────────────────────────────────────────────────────────────┤
 * │ M4A/MP4  │ MP4 atoms: ©nam, ©ART, ©alb, tmpo, ©lyr (plain JSON)          │
 * │          │   ----:com.apple.iTunes:SYLLABLE_LYRICS (JSON free-form atom)  │
 * │          │ covr atom (JPEG)                                               │
 * └──────────┴────────────────────────────────────────────────────────────────┘
 *
 * Lyrics JSON canonical format:
 *   [{"time": 4210, "text": "Primera línea"}, ...]
 *   time = milliseconds from start, text = lyric line / syllable
 *
 * Deezer LYRICS_SYNC_JSON is auto-converted via normalizeDeezerLyricsJson().
 *
 * Dependency (already in app/build.gradle.kts):
 *   implementation(libs.jaudiotagger)
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.images.PictureTypes
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AudioMetadataInjector {

    private const val TAG = "AudioMetadataInjector"

    // ── Tag key constants ──────────────────────────────────────────────────────

    /** Custom VorbisComment key written to FLAC files. */
    private const val VORBIS_SYLLABLE_KEY = "SYLLABLE_LYRICS"

    /** iTunes reverse-DNS namespace for the M4A free-form atom. */
    private const val M4A_ITUNES_DOMAIN = "com.apple.iTunes"

    /** Atom name inside the reverse-DNS namespace. */
    private const val M4A_SYLLABLE_KEY = "SYLLABLE_LYRICS"

    // ── Data model ────────────────────────────────────────────────────────────

    /**
     * All metadata to embed into the audio file.
     *
     * @param title             Track title.
     * @param artist            Artist / performer name.
     * @param album             Album / release title.
     * @param bpm               Beats-per-minute (0 = omit tag).
     * @param coverBytes        Raw JPEG bytes for front-cover art.
     * @param syllableLyricsJson  Canonical JSON string:
     *                          `[{"time":<ms>,"text":"<line>"},…]`
     *                          Pass null to skip lyrics injection.
     */
    data class TrackMetadata(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val bpm: Int = 0,
        val coverBytes: ByteArray? = null,
        val syllableLyricsJson: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TrackMetadata) return false
            return title == other.title &&
                artist == other.artist &&
                album == other.album &&
                bpm == other.bpm &&
                coverBytes.contentEquals(other.coverBytes ?: ByteArray(0)) &&
                syllableLyricsJson == other.syllableLyricsJson
        }
        override fun hashCode(): Int = title.hashCode() * 31 + artist.hashCode()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Inject [metadata] into [audioFile] and save it in-place (IO dispatcher).
     *
     * @return `true` on success, `false` on any error (errors are logged).
     */
    suspend fun inject(audioFile: File, metadata: TrackMetadata): Boolean =
        withContext(Dispatchers.IO) {
            require(audioFile.exists()) { "Audio file not found: ${audioFile.path}" }

            Log.d(TAG, "inject() → ${audioFile.name}")
            try {
                val af  = AudioFileIO.read(audioFile)
                val tag = af.tagOrCreateAndSetDefault

                embedTextTags(tag, metadata)
                embedCoverArt(tag, metadata.coverBytes)
                metadata.syllableLyricsJson?.let { injectLyrics(tag, it, audioFile.extension) }

                AudioFileIO.write(af)
                Log.d(TAG, "✅ Tags written: ${audioFile.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "inject() failed for ${audioFile.name}: $e")
                false
            }
        }

    // ── Core tag writers ──────────────────────────────────────────────────────

    private fun embedTextTags(tag: Tag, m: TrackMetadata) {
        if (m.title.isNotBlank())  tag.setField(FieldKey.TITLE,  m.title)
        if (m.artist.isNotBlank()) tag.setField(FieldKey.ARTIST, m.artist)
        if (m.album.isNotBlank())  tag.setField(FieldKey.ALBUM,  m.album)
        if (m.bpm > 0)             tag.setField(FieldKey.BPM,    m.bpm.toString())
        Log.d(TAG, "  Text tags: title='${m.title}' artist='${m.artist}' bpm=${m.bpm}")
    }

    private fun embedCoverArt(tag: Tag, coverBytes: ByteArray?) {
        coverBytes ?: return
        val artwork = AndroidArtwork().apply {
            binaryData  = coverBytes
            mimeType    = "image/jpeg"
            pictureType = PictureTypes.DEFAULT_ID   // 3 = front cover
        }
        runCatching {
            tag.deleteArtworkField()
            tag.setField(artwork)
            Log.d(TAG, "  Cover art embedded (${coverBytes.size} bytes)")
        }.onFailure {
            Log.e(TAG, "  embedCoverArt failed: $it")
        }
    }

    // ── Format-specific lyrics injection ──────────────────────────────────────

    private fun injectLyrics(tag: Tag, lyricsJson: String, ext: String) {
        when (tag) {
            is FlacTag -> injectFlacLyrics(tag, lyricsJson)
            is Mp4Tag  -> injectM4aLyrics(tag, lyricsJson)
            else -> runCatching {
                tag.setField(FieldKey.LYRICS, lyricsJson)
                Log.w(TAG, "  Lyrics stored in generic LYRICS field (ext=$ext)")
            }
        }
    }

    /**
     * FLAC: write two VorbisComment fields.
     *
     *  • `LYRICS`          — read by most external players.
     *  • `SYLLABLE_LYRICS` — read exclusively by Milla's syllable renderer.
     */
    private fun injectFlacLyrics(tag: FlacTag, lyricsJson: String) {
        // Standard LYRICS
        runCatching { tag.setField(FieldKey.LYRICS, lyricsJson) }
            .onFailure { Log.w(TAG, "  FLAC LYRICS field error: $it") }

        // Custom SYLLABLE_LYRICS
        runCatching {
            val field = tag.createField(VORBIS_SYLLABLE_KEY, lyricsJson)
            tag.addField(field)
        }.onFailure { Log.w(TAG, "  FLAC SYLLABLE_LYRICS field error: $it") }

        Log.d(TAG, "  [FLAC] LYRICS + SYLLABLE_LYRICS written (${lyricsJson.length} chars)")
    }

    /**
     * M4A: write two atoms.
     *
     *  • `©lyr`                                       — standard iTunes lyrics.
     *  • `----:com.apple.iTunes:SYLLABLE_LYRICS`      — Milla syllable JSON.
     */
    private fun injectM4aLyrics(tag: Mp4Tag, lyricsJson: String) {
        // Standard ©lyr
        runCatching { tag.setField(FieldKey.LYRICS, lyricsJson) }
            .onFailure { Log.w(TAG, "  M4A ©lyr error: $it") }

        // iTunes free-form reverse-DNS atom
        runCatching {
            val atom = Mp4TagReverseDnsField(
                Mp4TagReverseDnsField.IDENTIFIER,
                M4A_ITUNES_DOMAIN,
                M4A_SYLLABLE_KEY,
                lyricsJson,
            )
            tag.addField(atom)
        }.onFailure { Log.w(TAG, "  M4A SYLLABLE_LYRICS atom error: $it") }

        Log.d(TAG, "  [M4A] ©lyr + $M4A_ITUNES_DOMAIN:$M4A_SYLLABLE_KEY written (${lyricsJson.length} chars)")
    }

    // ── Lyrics JSON helpers ───────────────────────────────────────────────────

    /**
     * Convert Deezer `LYRICS_SYNC_JSON` format to Milla's canonical format.
     *
     * Deezer:   `[{"lrc_timestamp":"[00:04.21]","line":"text"}, …]`
     * Canonical:`[{"time":4210,"text":"text"}, …]`
     *
     * If the input is already canonical (has "time" key), it is returned as-is.
     */
    fun normalizeDeezerLyricsJson(deezerSyncJson: String): String {
        return try {
            val arr   = JSONArray(deezerSyncJson)
            val first = arr.optJSONObject(0) ?: return deezerSyncJson
            if (first.has("time")) return deezerSyncJson   // already canonical

            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val entry = arr.getJSONObject(i)
                val ts    = entry.optString("lrc_timestamp", "").trimStart('[').trimEnd(']')
                val ms    = parseLrcToMs(ts)
                val text  = entry.optString("line", "")
                out.put(JSONObject().apply {
                    put("time", ms)
                    put("text", text)
                })
            }
            out.toString()
        } catch (e: Exception) {
            Log.w(TAG, "normalizeDeezerLyricsJson parse error: $e")
            deezerSyncJson
        }
    }

    /**
     * Convert Tidal LRC-style string lyrics to Milla's canonical JSON.
     *
     * Tidal SYLT text field contains lines like `[mm:ss.xx] text`.
     * Returns canonical JSON array string.
     */
    fun normalizeTidalLyricsJson(tidalLyricsText: String): String {
        return try {
            val out   = JSONArray()
            val regex = Regex("""^\[(\d+:\d+\.\d+)]\s*(.*)$""")
            tidalLyricsText.lines().forEach { line ->
                val match = regex.matchEntire(line.trim()) ?: return@forEach
                val ms    = parseLrcToMs(match.groupValues[1])
                val text  = match.groupValues[2]
                out.put(JSONObject().apply {
                    put("time", ms)
                    put("text", text)
                })
            }
            out.toString()
        } catch (e: Exception) {
            Log.w(TAG, "normalizeTidalLyricsJson parse error: $e")
            "[]"
        }
    }

    /** Parse `mm:ss.xx` or `mm:ss.xxx` → milliseconds (Long). */
    private fun parseLrcToMs(ts: String): Long {
        return try {
            val (mm, ss) = ts.split(":")
            mm.toLong() * 60_000L + (ss.toDouble() * 1_000L).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Read the `SYLLABLE_LYRICS` field back from a tagged FLAC file.
     * Returns null if the field is absent.
     */
    fun readSyllableLyricsFromFlac(audioFile: File): String? {
        return try {
            val af  = AudioFileIO.read(audioFile)
            val tag = af.tag as? FlacTag ?: return null
            tag.getFirst(VORBIS_SYLLABLE_KEY).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "readSyllableLyricsFromFlac error: $e")
            null
        }
    }

    /**
     * Read the `SYLLABLE_LYRICS` free-form atom back from a tagged M4A file.
     * Returns null if the atom is absent.
     */
    fun readSyllableLyricsFromM4a(audioFile: File): String? {
        return try {
            val af  = AudioFileIO.read(audioFile)
            val tag = af.tag as? Mp4Tag ?: return null
            val key = "----:$M4A_ITUNES_DOMAIN:$M4A_SYLLABLE_KEY"
            tag.getFirst(key).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "readSyllableLyricsFromM4a error: $e")
            null
        }
    }
}
