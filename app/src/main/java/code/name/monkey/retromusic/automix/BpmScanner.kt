/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.network.RemoteTrackMetadata
import code.name.monkey.retromusic.network.SupabaseClientManager
import code.name.monkey.retromusic.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Contenedor de metadatos calculados/extraídos para Automix (DJ Crossfade & Harmonic Mixing).
 */
data class AutomixMetadata(
    val bpm: Float,
    val musicalKey: String,
    val replayGain: Float,
    val cueOutMs: Long,
    val cueInMs: Long = 0L,
    val isAnalyzed: Boolean = true
)

/**
 * Extension helper para enriquecer un [SongEntity] con los metadatos de Automix.
 */
fun SongEntity.withAutomixMetadata(
    bpm: Float = this.bpm,
    replayGain: Float = this.replayGain,
    musicalKey: String = this.musicalKey,
    cueOutMs: Long = this.cueOutMs,
    trackStartMs: Long = this.trackStartMs,
    trackEndMs: Long = this.trackEndMs,
    introSilenceDurationMs: Long = this.introSilenceDurationMs,
    outroSilenceDurationMs: Long = this.outroSilenceDurationMs,
    vocalStartMs: Long = this.vocalStartMs,
    vocalEndMs: Long = this.vocalEndMs,
    chorusStartMs: Long = this.chorusStartMs
): SongEntity {
    return SongEntity(
        songPrimaryKey = songPrimaryKey,
        playlistCreatorId = playlistCreatorId,
        id = id,
        title = title,
        trackNumber = trackNumber,
        year = year,
        duration = duration,
        data = data,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        composer = composer,
        albumArtist = albumArtist,
        bpm = bpm,
        replayGain = replayGain,
        musicalKey = musicalKey,
        trackStartMs = trackStartMs,
        trackEndMs = trackEndMs,
        introSilenceDurationMs = introSilenceDurationMs,
        outroSilenceDurationMs = outroSilenceDurationMs,
        vocalStartMs = vocalStartMs,
        vocalEndMs = vocalEndMs,
        chorusStartMs = chorusStartMs,
        cueOutMs = cueOutMs
    )
}

/**
 * Servicio en segundo plano para análisis y extracción de metadatos de audio (BPM, Musical Key,
 * ReplayGain y marcas CUE) orientado al motor de mezcla Automix multi-género.
 *
 * Implementa estrategia híbrida en 4 Capas:
 *  - Capa 0 (Room DB Local): Verificación inmediata offline si el tema ya cuenta con metadatos en DB.
 *  - Capa 1 (Supabase Global DB): Si no está local, consulta en milisegundos a la tabla track_metadata.
 *  - Capa 2 (ID3 Local / Deezer API): Lectura ID3 o enriquecimiento de API en caso de fallar red.
 *  - Capa 3 (Análisis RMS y Subida Asíncrona): Calcula punto RMS adaptativo y sube resultado a Supabase.
 */
object BpmScanner {

    private const val TAG = "BpmScanner"
    private const val DEFAULT_UNKNOWN_BPM = 0.0f
    private const val DEFAULT_UNKNOWN_CROSSFADE_MS = 5000L

    private val STANDARD_TO_CAMELOT = mapOf(
        "abm" to "1A", "g#m" to "1A", "b" to "1B",
        "ebm" to "2A", "d#m" to "2A", "f#" to "2B", "gb" to "2B",
        "bbm" to "3A", "a#m" to "3A", "db" to "3B", "c#" to "3B",
        "fm" to "4A", "ab" to "4B", "g#" to "4B",
        "cm" to "5A", "eb" to "5B", "d#" to "5B",
        "gm" to "6A", "bb" to "6B", "a#" to "6B",
        "dm" to "7A", "f" to "7B",
        "am" to "8A", "c" to "8B",
        "em" to "9A", "g" to "9B",
        "bm" to "10A", "d" to "10B",
        "f#m" to "11A", "gbm" to "11A", "a" to "11B",
        "c#m" to "12A", "dbm" to "12A", "e" to "12B"
    )

    /**
     * Genera un identificador universal único para consultar e indexar en la tabla track_metadata de Supabase.
     */
    fun generateTrackId(artist: String, title: String, fallbackId: Long = 0L): String {
        val cleanArtist = artist.trim().lowercase(Locale.ROOT)
        val cleanTitle = title.trim().lowercase(Locale.ROOT)
        if (cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty()) {
            return "${cleanArtist}__${cleanTitle}".replace(Regex("[^a-z0-9_]"), "_")
        }
        return "track_${fallbackId}"
    }

    /**
     * Analiza el archivo de audio especificado integrando la estrategia híbrida de Supabase.
     */
    suspend fun scanFile(
        filePath: String,
        durationMs: Long = 0L,
        artist: String = "",
        title: String = "",
        genre: String = "",
        songId: Long = 0L,
        repository: RoomRepository? = null
    ): AutomixMetadata = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val actualDurationMs = if (durationMs > 0L) durationMs else 0L

        // a) Capa Supabase Global: Si el archivo es válido y hay título/artista, consultar Supabase
        val trackId = generateTrackId(artist, title, songId)
        val remoteMeta = if (artist.isNotBlank() && title.isNotBlank()) {
            SupabaseClientManager.fetchMetadata(trackId)
        } else null

        val remoteBpm = remoteMeta?.bpm ?: DEFAULT_UNKNOWN_BPM
        if (remoteMeta != null && remoteBpm > 0f) {
            if (repository != null && songId != 0L) {
                try {
                    repository.updateSongAutomixData(
                        songId = songId,
                        bpm = remoteBpm,
                        key = remoteMeta.musicalKey.orEmpty(),
                        replayGain = remoteMeta.replayGain,
                        cueOut = remoteMeta.cueOutMs
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error persistiendo datos de Supabase a Room: ${e.message}")
                }
            }
            return@withContext AutomixMetadata(
                bpm = remoteBpm,
                musicalKey = remoteMeta.musicalKey.orEmpty(),
                replayGain = remoteMeta.replayGain,
                cueOutMs = remoteMeta.cueOutMs,
                cueInMs = 0L,
                isAnalyzed = true
            )
        }

        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "No se puede leer el archivo de audio: $filePath")
            val failedMeta = buildFailedMetadata()
            if (repository != null && songId != 0L) {
                try {
                    repository.updateSongAutomixData(
                        songId = songId,
                        bpm = failedMeta.bpm,
                        key = failedMeta.musicalKey,
                        replayGain = failedMeta.replayGain,
                        cueOut = failedMeta.cueOutMs
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando Room DB para fallido: ${e.message}")
                }
            }
            return@withContext failedMeta
        }

        try {
            val audioFile = try {
                AudioFileIO.read(file)
            } catch (e: Exception) {
                null
            }
            if (audioFile == null) {
                Log.w(TAG, "No se pudo leer tags de audio con AudioFileIO: $filePath")
                val failedMeta = buildFailedMetadata()
                if (repository != null && songId != 0L) {
                    try {
                        repository.updateSongAutomixData(
                            songId = songId,
                            bpm = failedMeta.bpm,
                            key = failedMeta.musicalKey,
                            replayGain = failedMeta.replayGain,
                            cueOut = failedMeta.cueOutMs
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error actualizando Room DB para tags fallidos: ${e.message}")
                    }
                }
                return@withContext failedMeta
            }
            val tag: Tag? = audioFile.tag
            val headerDurationMs = if (actualDurationMs > 0L) {
                actualDurationMs
            } else {
                (audioFile?.audioHeader?.trackLength?.toLong() ?: 0L) * 1000L
            }

            // Nivel 1: Lectura ID3 Local
            var bpm = extractLocalBpm(tag)
            var replayGain = extractReplayGain(tag)
            val musicalKey = extractKey(tag, file)
            val extractedGenre = if (genre.isNotBlank()) genre else tryReadField(tag, FieldKey.GENRE)

            // Nivel 2: Enriquecimiento Cloud vía Deezer API si no existe BPM local
            if ((bpm <= 0f || bpm.isNaN()) && (artist.isNotBlank() || title.isNotBlank())) {
                val cloudData = fetchDeezerMetadata(artist, title)
                if (cloudData != null) {
                    if (cloudData.first > 0f) {
                        bpm = cloudData.first
                    }
                    if (replayGain == 0f && cloudData.second != 0f) {
                        replayGain = cloudData.second
                    }
                }
            }

            if (bpm < 50f || bpm > 250f || bpm.isNaN()) {
                bpm = DEFAULT_UNKNOWN_BPM
            }

            // Cálculo adaptativo multi-género de cueOutMs con análisis de energía RMS
            val cueOutMs = calculateAdaptiveCueOutMs(tag, file, bpm, headerDurationMs, extractedGenre)

            val metadataResult = AutomixMetadata(
                bpm = bpm,
                musicalKey = musicalKey,
                replayGain = replayGain,
                cueOutMs = cueOutMs,
                cueInMs = 0L,
                isAnalyzed = true
            )

            // Persistencia local inmediata a Room si se suministró el repositorio
            if (repository != null && songId != 0L) {
                try {
                    repository.updateSongAutomixData(
                        songId = songId,
                        bpm = bpm,
                        key = musicalKey,
                        replayGain = replayGain,
                        cueOut = cueOutMs
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando Room DB en scanFile: ${e.message}")
                }
            }

            // c) Subir datos calculados a Supabase en segundo plano para enriquecer la DB global
            if (artist.isNotBlank() && title.isNotBlank() && bpm > 0f) {
                GlobalScope.launch(Dispatchers.IO) {
                    SupabaseClientManager.uploadMetadata(
                        RemoteTrackMetadata(
                            trackId = trackId,
                            title = title.trim(),
                            artist = artist.trim(),
                            bpm = bpm,
                            musicalKey = musicalKey,
                            cueOutMs = cueOutMs,
                            replayGain = replayGain
                        )
                    )
                }
            }

            metadataResult
        } catch (e: Exception) {
            Log.e(TAG, "Error en scanFile ($filePath): ${e.message}")
            buildUnknownMetadata(actualDurationMs)
        }
    }

    /**
     * Analiza una canción [Song] y devuelve sus metadatos de Automix bajo estrategia híbrida.
     */
    suspend fun scanSong(song: Song): AutomixMetadata {
        return scanFile(
            filePath = song.data,
            durationMs = song.duration,
            artist = song.artistName,
            title = song.title,
            songId = song.id
        )
    }

    /**
     * Analiza una canción [Song] comprobando primero Room local y Supabase antes del análisis físico.
     */
    suspend fun scanAndSaveSong(song: Song, repository: RoomRepository? = null): AutomixMetadata {
        return scanFile(
            filePath = song.data,
            durationMs = song.duration,
            artist = song.artistName,
            title = song.title,
            songId = song.id,
            repository = repository
        )
    }

    /**
     * Analiza un [SongEntity] bajo estrategia híbrida y actualiza Room + Supabase en background.
     */
    suspend fun scanSongEntity(
        entity: SongEntity,
        repository: RoomRepository? = null
    ): SongEntity {
        // a) Verificación en DB local (Room DB first)
        if (entity.bpm > 0f) {
            return entity
        }
        val metadata = scanFile(
            filePath = entity.data,
            durationMs = entity.duration,
            artist = entity.artistName,
            title = entity.title,
            songId = entity.id,
            repository = repository
        )
        return entity.withAutomixMetadata(
            bpm = metadata.bpm,
            replayGain = metadata.replayGain,
            musicalKey = metadata.musicalKey,
            cueOutMs = metadata.cueOutMs
        )
    }

    suspend fun scanSongEntities(
        entities: List<SongEntity>,
        repository: RoomRepository? = null,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val total = entities.size
        entities.mapIndexed { index, entity ->
            val updated = scanSongEntity(entity, repository)
            onProgress?.invoke(index + 1, total)
            updated
        }
    }

    fun toCamelotKey(keyString: String): String {
        val cleaned = keyString.trim().lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("minor", "m")
            .replace("major", "")
            .replace("min", "m")
            .replace("maj", "")

        if (cleaned.matches(Regex("^[1-9]a|b$|^1[0-2]a|b$"))) {
            return cleaned.uppercase(Locale.ROOT)
        }

        return STANDARD_TO_CAMELOT[cleaned] ?: keyString.trim().uppercase(Locale.ROOT)
    }

    fun isHarmonicallyCompatible(key1: String, key2: String): Boolean {
        val cam1 = toCamelotKey(key1)
        val cam2 = toCamelotKey(key2)
        if (cam1.isEmpty() || cam2.isEmpty()) return true

        if (cam1 == cam2) return true

        val num1 = cam1.filter { it.isDigit() }.toIntOrNull() ?: return true
        val letter1 = cam1.filter { it.isLetter() }
        val num2 = cam2.filter { it.isDigit() }.toIntOrNull() ?: return true
        val letter2 = cam2.filter { it.isLetter() }

        if (num1 == num2 && letter1 != letter2) return true

        if (letter1 == letter2) {
            val diff = abs(num1 - num2)
            val distance = min(diff, 12 - diff)
            if (distance <= 1) return true
        }

        return false
    }

    private fun extractLocalBpm(tag: Tag?): Float {
        if (tag == null) return 0f
        val rawBpm = tryReadField(tag, FieldKey.BPM)
        val parsed = rawBpm.replace(",", ".").toFloatOrNull()
        if (parsed != null && parsed in 50f..250f) {
            return (parsed * 10f).roundToInt() / 10f
        }
        return 0f
    }

    private fun extractKey(tag: Tag?, file: File): String {
        if (tag != null) {
            val rawKey = tryReadField(tag, FieldKey.KEY)
            if (rawKey.isNotEmpty()) {
                return toCamelotKey(rawKey)
            }
            val comment = tryReadField(tag, FieldKey.COMMENT)
            val camelotMatch = Regex("\\b([1-9]|1[0-2])[ABab]\\b").find(comment)
            if (camelotMatch != null) {
                return camelotMatch.value.uppercase(Locale.ROOT)
            }
        }
        return ""
    }

    private fun extractReplayGain(tag: Tag?): Float {
        if (tag == null) return 0f
        val possibleKeys = listOf("REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_ALBUM_GAIN", "rva2")
        for (key in possibleKeys) {
            val value = try {
                tag.getFirst(key)?.trim()
            } catch (e: Exception) {
                null
            }
            if (!value.isNullOrEmpty()) {
                val cleaned = value.replace("dB", "").replace("db", "").trim()
                val parsed = cleaned.replace(",", ".").toFloatOrNull()
                if (parsed != null) return parsed
            }
        }
        return 0f
    }

    private suspend fun fetchDeezerMetadata(artist: String, title: String): Pair<Float, Float>? =
        withContext(Dispatchers.IO) {
            try {
                val cleanArtist = artist.trim()
                val cleanTitle = title.trim()
                val queryStr = if (cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty()) {
                    "artist:\"$cleanArtist\" track:\"$cleanTitle\""
                } else {
                    "$cleanArtist $cleanTitle".trim()
                }
                if (queryStr.isBlank()) return@withContext null

                val encodedQuery = URLEncoder.encode(queryStr, "UTF-8")
                val searchUrl = URL("https://api.deezer.com/search?q=$encodedQuery&limit=1")
                val connection = (searchUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "RetroMusic-Milla/1.0")
                }

                if (connection.responseCode != 200) return@withContext null
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val dataArray = json.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val firstTrack = dataArray.getJSONObject(0)
                    val trackId = firstTrack.optLong("id", -1L)
                    var bpm = firstTrack.optDouble("bpm", 0.0).toFloat()
                    var gain = firstTrack.optDouble("gain", 0.0).toFloat()

                    if ((bpm <= 0f || bpm.isNaN()) && trackId > 0L) {
                        val trackUrl = URL("https://api.deezer.com/track/$trackId")
                        val trackConn = (trackUrl.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 2500
                            readTimeout = 2500
                            requestMethod = "GET"
                            setRequestProperty("User-Agent", "RetroMusic-Milla/1.0")
                        }

                        if (trackConn.responseCode == 200) {
                            val trackJson = JSONObject(trackConn.inputStream.bufferedReader().use { it.readText() })
                            bpm = trackJson.optDouble("bpm", 0.0).toFloat()
                            gain = trackJson.optDouble("gain", 0.0).toFloat()
                        }
                    }

                    if (bpm > 0f && !bpm.isNaN()) {
                        return@withContext Pair(bpm, gain)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Deezer Cloud Enriquecimiento no disponible: ${e.message}")
            }
            null
        }

    private fun calculateAdaptiveCueOutMs(
        tag: Tag?,
        file: File,
        bpm: Float,
        durationMs: Long,
        genre: String
    ): Long {
        if (durationMs <= 0L) return 0L

        val cueComment = tryReadField(tag, FieldKey.COMMENT)
        val cueMatch = Regex("cue_out_ms=(\\d+)").find(cueComment)
        if (cueMatch != null) {
            val explicitCue = cueMatch.groupValues[1].toLongOrNull()
            if (explicitCue != null && explicitCue in 1000L..durationMs) {
                return explicitCue
            }
        }

        if (bpm <= 0f || bpm.isNaN()) {
            return (durationMs - DEFAULT_UNKNOWN_CROSSFADE_MS).coerceAtLeast(0L)
        }

        val lowerGenre = genre.lowercase(Locale.ROOT)
        val (minFadeMs, maxFadeMs, defaultFadeMs) = when {
            lowerGenre.contains("salsa") || lowerGenre.contains("merengue") ||
                lowerGenre.contains("bomba") || lowerGenre.contains("bachata") ||
                lowerGenre.contains("latin") || lowerGenre.contains("latina") ||
                lowerGenre.contains("cumbia") || lowerGenre.contains("tropical") ||
                lowerGenre.contains("mambo") || lowerGenre.contains("timba") -> {
                Triple(3000L, 6000L, 4500L)
            }
            lowerGenre.contains("pop") || lowerGenre.contains("reguet") ||
                lowerGenre.contains("reggaet") || lowerGenre.contains("electr") ||
                lowerGenre.contains("dance") || lowerGenre.contains("edm") ||
                lowerGenre.contains("house") || lowerGenre.contains("techno") ||
                lowerGenre.contains("k-pop") || lowerGenre.contains("kpop") -> {
                Triple(6000L, 10000L, 8000L)
            }
            else -> {
                Triple(5000L, 8000L, 6500L)
            }
        }

        val rmsDropOffset = detectRmsDropOffsetMs(file, durationMs, minFadeMs, maxFadeMs)
        if (rmsDropOffset != null) {
            return rmsDropOffset
        }

        val beatMs = (60_000f / bpm).toLong()
        val beatsInFade = ((defaultFadeMs.toFloat() / beatMs).roundToInt()).coerceAtLeast(4)
        val alignedFadeMs = (beatsInFade * beatMs).coerceIn(minFadeMs, maxFadeMs)

        return (durationMs - alignedFadeMs).coerceAtLeast(0L)
    }

    private fun detectRmsDropOffsetMs(
        file: File,
        durationMs: Long,
        minFadeMs: Long,
        maxFadeMs: Long
    ): Long? {
        if (durationMs < 20_000L || !file.exists() || file.length() < 100_000L) {
            return null
        }
        try {
            val fileLen = file.length()
            val startSearchMs = (durationMs - 20_000L).coerceAtLeast(0L)
            val startByte = ((startSearchMs.toDouble() / durationMs.toDouble()) * fileLen).toLong()

            BufferedInputStream(FileInputStream(file)).use { stream ->
                stream.skip(startByte)
                val bufferSize = 8192
                val buffer = ByteArray(bufferSize)
                val numWindows = 20
                val rmsValues = FloatArray(numWindows)
                var bytesReadTotal = 0L
                val bytesPerWindow = ((fileLen - startByte) / numWindows).coerceAtLeast(1L)

                for (w in 0 until numWindows) {
                    var sumSquares = 0.0
                    var sampleCount = 0
                    var windowBytesRead = 0L
                    while (windowBytesRead < bytesPerWindow) {
                        val read = stream.read(buffer, 0, min((bytesPerWindow - windowBytesRead).toInt(), bufferSize))
                        if (read <= 0) break
                        for (i in 0 until read step 2) {
                            if (i + 1 < read) {
                                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                                val norm = sample.toDouble() / 32768.0
                                sumSquares += norm * norm
                                sampleCount++
                            }
                        }
                        windowBytesRead += read
                        bytesReadTotal += read
                    }
                    val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                    rmsValues[w] = rms
                }

                var refRms = 0f
                for (w in 0 until (numWindows / 2)) {
                    if (rmsValues[w] > refRms) refRms = rmsValues[w]
                }
                if (refRms <= 0.005f) return null

                val threshold = refRms * 0.55f
                for (w in (numWindows - 1) downTo 0) {
                    val windowTimeMs = startSearchMs + ((w.toDouble() / numWindows.toDouble()) * 20_000L).toLong()
                    val timeFromEndMs = durationMs - windowTimeMs
                    if (timeFromEndMs in minFadeMs..maxFadeMs && rmsValues[w] < threshold) {
                        return windowTimeMs
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar
        }
        return null
    }

    private fun tryReadField(tag: Tag?, fieldKey: FieldKey): String {
        if (tag == null) return ""
        return try {
            tag.getFirst(fieldKey)?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildUnknownMetadata(durationMs: Long): AutomixMetadata {
        val fallbackCueOut = (durationMs - DEFAULT_UNKNOWN_CROSSFADE_MS).coerceAtLeast(0L)
        return AutomixMetadata(
            bpm = DEFAULT_UNKNOWN_BPM,
            musicalKey = "",
            replayGain = 0f,
            cueOutMs = fallbackCueOut,
            cueInMs = 0L,
            isAnalyzed = false
        )
    }

    private fun buildFailedMetadata(): AutomixMetadata {
        return AutomixMetadata(
            bpm = -1f,
            musicalKey = "FAILED",
            replayGain = 0f,
            cueOutMs = -1L,
            cueInMs = 0L,
            isAnalyzed = false
        )
    }
}
