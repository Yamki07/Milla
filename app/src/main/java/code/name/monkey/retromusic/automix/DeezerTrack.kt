/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Engine
 * Translated & adapted from ReFreezer (DJDoubleD) — Dart → Kotlin
 */
package code.name.monkey.retromusic.automix

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model for a Deezer track, directly mapped from the private GW-light API.
 * Field names match the JSON keys returned by deezer.pageSearch.
 */
data class DeezerTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val albumCoverId: String,   // e.g. "abc123" → usado para construir la URL de portada
    val durationSec: Int,
    val explicit: Boolean,
    val md5Origin: String,      // Necesario para construir la URL de stream
    val mediaVersion: String,   // Necesario para construir la URL de stream
    val trackToken: String,     // Token de corta vida para autenticación CDN
    val fileSize320: Long = 0L,
    val fileSize128: Long = 0L,
    val fileFlac: Long = 0L,
    // ── Metadatos enriquecidos para tagging ID3 perfecto ──
    val isrc: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val bpmFromApi: Float = 0f,     // BPM directo de Deezer API
    val gainFromApi: Float = 0f,    // Gain (ReplayGain-like) de Deezer API
    val syncedLrcJson: String = "", // JSON de letras sincronizadas de Deezer GW
    val unsyncedLyrics: String = "",// Letras planas sin timestamp
) {
    /** Full cover art URL (1000x1000) */
    val coverUrlFull: String
        get() = "https://e-cdns-images.dzcdn.net/images/cover/$albumCoverId/1000x1000-000000-80-0-0.jpg"

    /** Thumbnail cover art URL (264x264) */
    val coverUrlThumb: String
        get() = "https://e-cdns-images.dzcdn.net/images/cover/$albumCoverId/264x264-000000-80-0-0.jpg"

    /** Human-readable duration string, e.g. "3:45" */
    val durationString: String
        get() {
            val mins = durationSec / 60
            val secs = durationSec % 60
            return "$mins:${secs.toString().padStart(2, '0')}"
        }

    /** Best available quality label */
    val qualityLabel: String
        get() = when {
            fileFlac > 0L -> "FLAC"
            fileSize320 > 0L -> "320"
            else -> "128"
        }

    companion object {
        /**
         * Parse a single track from the private JSON response.
         * Matching fields from ReFreezer definitions.dart → Track.fromPrivateJson
         */
        fun fromPrivateJson(json: JSONObject): DeezerTrack {
            val sngId = json.optString("SNG_ID", "")
            var title = json.optString("SNG_TITLE", "Unknown")
            val version = json.optString("VERSION", "")
            if (version.isNotBlank()) title = "$title $version"

            // Artists: prefer ARTISTS array, fallback to single ART_NAME
            val artistName: String = try {
                val arr = json.optJSONArray("ARTISTS")
                if (arr != null && arr.length() > 0) {
                    (0 until arr.length()).joinToString(", ") { i ->
                        arr.getJSONObject(i).optString("ART_NAME", "")
                    }
                } else {
                    json.optString("ART_NAME", "Unknown Artist")
                }
            } catch (e: Exception) {
                json.optString("ART_NAME", "Unknown Artist")
            }

            val albTitle = json.optString("ALB_TITLE", "")
            val albPicture = json.optString("ALB_PICTURE", "")
            val duration = json.optString("DURATION", "0").toIntOrNull() ?: 0
            val explicit = json.optString("EXPLICIT_LYRICS", "0") == "1"

            // Playback details for stream URL construction (see DeezerDecryptor)
            val md5 = json.optString("MD5_ORIGIN", "")
            val mediaVersion = json.optString("MEDIA_VERSION", "")
            val trackToken = json.optString("TRACK_TOKEN", "")

            val fs320 = json.optLong("FILESIZE_MP3_320", 0L)
            val fs128 = json.optLong("FILESIZE_MP3_128", 0L)
            val fsFlac = json.optLong("FILESIZE_FLAC", 0L)

            // Metadatos enriquecidos
            val isrc = json.optString("ISRC", "")
            val albumArtist = try {
                json.optJSONObject("ALBUM")?.let { album ->
                    album.optJSONArray("ARTISTS")?.let { arr ->
                        if (arr.length() > 0) arr.getJSONObject(0).optString("ART_NAME", "")
                        else null
                    }
                } ?: json.optString("ALB_ART_NAME", "")
            } catch (e: Exception) { "" }
            val composer = try {
                json.optJSONArray("SNG_CONTRIBUTORS")?.let { arr ->
                    (0 until arr.length())
                        .map { i -> arr.getJSONObject(i).optString("name", "") }
                        .filter { it.isNotEmpty() }
                        .take(3)
                        .joinToString(", ")
                } ?: ""
            } catch (e: Exception) { "" }
            val trackNumber = json.optString("TRACK_NUMBER", "0").toIntOrNull() ?: 0
            val discNumber = json.optString("DISK_NUMBER", "1").toIntOrNull() ?: 1
            val year = json.optString("PHYSICAL_RELEASE_DATE", "").take(4).toIntOrNull() ?: 0
            val genre = try {
                json.optJSONObject("GENRES")?.optJSONArray("data")
                    ?.let { arr -> if (arr.length() > 0) arr.getJSONObject(0).optString("name", "") else "" }
                    ?: ""
            } catch (e: Exception) { "" }
            val bpmFromApi = json.optDouble("BPM", 0.0).toFloat()
            val gainFromApi = json.optDouble("GAIN", 0.0).toFloat()

            return DeezerTrack(
                id = sngId,
                title = title,
                artistName = artistName,
                albumTitle = albTitle,
                albumCoverId = albPicture,
                durationSec = duration,
                explicit = explicit,
                md5Origin = md5,
                mediaVersion = mediaVersion,
                trackToken = trackToken,
                fileSize320 = fs320,
                fileSize128 = fs128,
                fileFlac = fsFlac,
                isrc = isrc,
                albumArtist = albumArtist,
                composer = composer,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year,
                genre = genre,
                bpmFromApi = bpmFromApi,
                gainFromApi = gainFromApi,
            )
        }

        /**
         * Parse the full search results list from the GW-light API.
         * Path: data['results']['TRACK']['data']
         */
        fun listFromSearchJson(json: JSONObject): List<DeezerTrack> {
            return try {
                val tracksArr: JSONArray = json
                    .getJSONObject("results")
                    .getJSONObject("TRACK")
                    .getJSONArray("data")
                (0 until tracksArr.length()).map { i ->
                    fromPrivateJson(tracksArr.getJSONObject(i))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
