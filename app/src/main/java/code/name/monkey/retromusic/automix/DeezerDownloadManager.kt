/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.content.Context
import android.os.Environment
import android.util.Log
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.AndroidArtwork
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.repository.RoomRepository
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume


/**
 * Gestor moderno de descargas nativo para Deezer con Corrutinas y Flow.
 * Traduce la lógica de ReFreezer (Download.java / DownloadService.java / DownloadsDatabase.java),
 * integrando desencriptación al vuelo con [DeezerDecryptor.decryptChunk] y guardado en almacenamiento físico
 * en alta calidad (FLAC / MP3 320kbps) para uso offline en Automix.
 */
object DeezerDownloadManager {
    private const val TAG = "DeezerDownloadManager"

    enum class Status {
        NONE,
        DOWNLOADING,
        POST_PROCESSING,
        DONE,
        ERROR
    }

    sealed class DownloadState(val status: Status) {
        object Idle : DownloadState(Status.NONE)
        data class Downloading(val trackId: String, val progress: Int) : DownloadState(Status.DOWNLOADING)
        data class PostProcessing(val trackId: String) : DownloadState(Status.POST_PROCESSING)
        data class Completed(val trackId: String, val filePath: String, val song: Song) : DownloadState(Status.DONE)
        data class Error(val trackId: String, val message: String) : DownloadState(Status.ERROR)
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val httpClient = OkHttpClient()

    /**
     * Alias para compatibilidad con la UI de Milla / Deezer.
     */
    fun downloadTrack(context: Context, song: Song, quality: Int = 9) {
        startDownload(context, song, quality)
    }

    /**
     * Inicia la descarga y desencriptación de una canción [song] desde Deezer.
     * @param quality 9 para FLAC, 3 para MP3 320kbps.
     */
    fun startDownload(context: Context, song: Song, quality: Int = 9) {
        val trackId = song.id.toString()
        if (activeJobs.containsKey(trackId)) {
            Log.d(TAG, "Descarga ya en curso para trackId=$trackId")
            return
        }

        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                _downloadState.value = DownloadState.Downloading(trackId, 0)
                Log.d(TAG, "Iniciando descarga de Deezer para: ${song.title} ($trackId)")

                val streamUrl = fetchUrlSuspending(trackId, quality)
                if (streamUrl.isNullOrEmpty()) {
                    _downloadState.value = DownloadState.Error(trackId, "No se pudo obtener el enlace de Deezer")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error: Token expirado o URL no encontrada", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val downloadDir = getDownloadsDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val extension = if (quality == 9) "flac" else "mp3"
                val fileName = "${sanitizeFileName(song.artistName)} - ${sanitizeFileName(song.title)}.$extension"
                val outputFile = File(downloadDir, fileName)

                val success = downloadAndDecrypt(song, streamUrl, outputFile)
                if (success) {
                    _downloadState.value = DownloadState.PostProcessing(trackId)
                    tagAndEnrichDownloadedFile(context, outputFile, song)
                    val mimeType = if (quality == 9) "audio/flac" else "audio/mpeg"
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf(mimeType)) { path, uri ->
                        Log.i(TAG, "MediaScanner completado para: $path, uri=$uri")
                    }
                    _downloadState.value = DownloadState.Completed(trackId, outputFile.absolutePath, song)
                    Log.i(TAG, "Descarga completada: ${outputFile.absolutePath}")

                    // Silent anonymous metadata contribution to Supabase Automix system
                    try {
                        if (code.name.monkey.retromusic.fragments.settings.MillaySettingsFragment.isContributeMetadata(context)) {
                            val cleanArtist = song.artistName.filter { it.isLetterOrDigit() || it.isWhitespace() }.replace(" ", "_").lowercase()
                            val cleanTitle = song.title.filter { it.isLetterOrDigit() || it.isWhitespace() }.replace(" ", "_").lowercase()
                            val millaId = "${cleanArtist}_${cleanTitle}"
                            code.name.monkey.retromusic.network.SupabaseClientManager.insertTrackMetadata(
                                listOf(mapOf(
                                    "track_id" to millaId,
                                    "title" to song.title,
                                    "artist" to song.artistName,
                                    "album" to song.albumName,
                                    "quality_format" to if (quality == 9) "flac" else "mp3_320",
                                    "duration_ms" to song.duration
                                ))
                            )
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Skipped metadata contribution: ${e.message}")
                    }

                } else {
                    if (outputFile.exists()) outputFile.delete()
                    _downloadState.value = DownloadState.Error(trackId, "Fallo al descargar o desencriptar el archivo")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error: Descarga o desencriptación falló", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en descarga de trackId=$trackId: $e")
                _downloadState.value = DownloadState.Error(trackId, e.message ?: "Error desconocido")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                activeJobs.remove(trackId)
            }
        }
        activeJobs[trackId] = job
    }

    /**
     * Cancela la descarga en curso para el [trackId].
     */
    fun cancelDownload(trackId: String) {
        activeJobs[trackId]?.cancel()
        activeJobs.remove(trackId)
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Directorio físico donde se almacenarán las descargas de Milla / Deezer.
     */
    fun getDownloadsDirectory(context: Context): File {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return File(baseDir, "RetroMusic")
    }

    private suspend fun fetchUrlSuspending(trackId: String, quality: Int): String? =
        suspendCancellableCoroutine { continuation ->
            DeezerApiClient.fetchStreamUrl(trackId, quality) { url ->
                if (continuation.isActive) {
                    continuation.resume(url)
                }
            }
        }

    private suspend fun downloadAndDecrypt(song: Song, url: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext false
                }

                val body = response.body ?: run {
                    response.close()
                    return@withContext false
                }
                val contentLength = body.contentLength()
                val trackKey = DeezerDecryptor.getKey(song.id.toString())

                val inputStream = body.byteStream()
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(2048)
                    var chunkIndex = 0
                    var bytesDownloaded = 0L

                    while (true) {
                        var bytesReadInChunk = 0
                        while (bytesReadInChunk < 2048) {
                            val read = inputStream.read(
                                buffer,
                                bytesReadInChunk,
                                2048 - bytesReadInChunk
                            )
                            if (read == -1) break
                            bytesReadInChunk += read
                        }
                        if (bytesReadInChunk == 0) break

                        val dataToWrite = if (bytesReadInChunk == 2048 && chunkIndex % 3 == 0) {
                            DeezerDecryptor.decryptChunk(trackKey, buffer)
                        } else {
                            if (bytesReadInChunk < 2048) {
                                buffer.copyOfRange(0, bytesReadInChunk)
                            } else {
                                buffer
                            }
                        }
                        outputStream.write(dataToWrite, 0, bytesReadInChunk)
                        chunkIndex++
                        bytesDownloaded += bytesReadInChunk
                        
                        if (contentLength > 0) {
                            val percent = ((bytesDownloaded * 100) / contentLength).toInt()
                            // Solo emitir cada cierto % para no saturar el StateFlow
                            if (percent % 5 == 0) {
                                _downloadState.value = DownloadState.Downloading(song.id.toString(), percent)
                            }
                        }

                        if (bytesReadInChunk < 2048) break
                    }
                    
                    // Final progress
                    _downloadState.value = DownloadState.Downloading(song.id.toString(), 100)
                    outputStream.flush()
                }
                response.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error durante downloadAndDecrypt: $e")
                false
            }
        }

    private suspend fun tagAndEnrichDownloadedFile(context: Context, outputFile: File, song: Song) =
        withContext(Dispatchers.IO) {
            // 1. Obtener datos privados completos de Deezer (ISRC, composer, año, genre, etc.)
            val privateTrack = try {
                DeezerApiClient.fetchPrivateTrackData(song.id.toString())
            } catch (e: Exception) { null }

            // 2. Obtener letras: primero sincronizadas (LRC) luego planas como fallback
            val (syncedLrc, plainLyrics) = try {
                val pair = DeezerApiClient.getLyricsFullPair(song.id.toString())
                // Si Deezer no tiene letras, cascadear a otras fuentes
                if (pair.first.isEmpty() && pair.second.isEmpty()) {
                    val lrcFromAmll = code.name.monkey.retromusic.util.AmllLyricsFetcher.fetchLyrics(song.title, song.artistName) ?: ""
                    val lrcFromMusixmatch = if (lrcFromAmll.isEmpty()) {
                        code.name.monkey.retromusic.util.MusixmatchFetcher.getEnhancedLrc(song.title, song.artistName) ?: ""
                    } else ""
                    val lrcFromLib = if (lrcFromAmll.isEmpty() && lrcFromMusixmatch.isEmpty()) {
                        code.name.monkey.retromusic.util.LRCLibFetcher.fetchLyrics(song) ?: ""
                    } else ""
                    val best = lrcFromAmll.ifEmpty { lrcFromMusixmatch.ifEmpty { lrcFromLib } }
                    Pair(best, "")
                } else pair
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo letras: ${e.message}")
                Pair("", "")
            }

            // 3. Etiquetado físico ID3 completo con JAudioTagger
            try {
                org.jaudiotagger.tag.TagOptionSingleton.getInstance().isAndroid = true

                var audioFile: org.jaudiotagger.audio.AudioFile? = null
                try {
                    audioFile = AudioFileIO.read(outputFile)
                } catch (e: Exception) {
                    try {
                        if (outputFile.name.endsWith(".mp3", ignoreCase = true)) {
                            audioFile = org.jaudiotagger.audio.mp3.MP3File(outputFile)
                        } else if (outputFile.name.endsWith(".flac", ignoreCase = true)) {
                            audioFile = org.jaudiotagger.audio.flac.FlacFileReader().read(outputFile)
                        }
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallo fallback de lectura: ${e2.message}")
                    }
                }

                if (audioFile == null) {
                    Log.e(TAG, "No se pudo inicializar AudioFile para ${outputFile.name}")
                    return@withContext
                }

                // Usar ID3v2.3 para MP3 (máxima compatibilidad) o default para FLAC
                var tag = audioFile.tag
                if (tag == null || (audioFile is org.jaudiotagger.audio.mp3.MP3File && tag is org.jaudiotagger.tag.id3.ID3v24Tag)) {
                    if (audioFile is org.jaudiotagger.audio.mp3.MP3File) {
                        tag = org.jaudiotagger.tag.id3.ID3v23Tag()
                        audioFile.tag = tag
                    } else {
                        tag = audioFile.createDefaultTag()
                        audioFile.tag = tag
                    }
                }

                // ── Tags básicos obligatorios ──
                tag.setField(FieldKey.TITLE, song.title)
                tag.setField(FieldKey.ARTIST, song.artistName)
                tag.setField(FieldKey.ALBUM, song.albumName)

                // ── Tags enriquecidos de Deezer ──
                val effectiveAlbumArtist = privateTrack?.albumArtist?.takeIf { it.isNotEmpty() } ?: song.albumArtist ?: song.artistName
                val effectiveComposer = privateTrack?.composer?.takeIf { it.isNotEmpty() } ?: song.composer ?: ""
                val effectiveYear = if (privateTrack?.year != null && privateTrack.year > 0) privateTrack.year else song.year
                val effectiveTrackNum = if (privateTrack?.trackNumber != null && privateTrack.trackNumber > 0) privateTrack.trackNumber else song.trackNumber
                val effectiveDisc = privateTrack?.discNumber ?: 1
                val effectiveGenre = privateTrack?.genre?.takeIf { it.isNotEmpty() } ?: ""
                val effectiveIsrc = privateTrack?.isrc?.takeIf { it.isNotEmpty() } ?: ""

                if (effectiveAlbumArtist.isNotEmpty()) {
                    try { tag.setField(FieldKey.ALBUM_ARTIST, effectiveAlbumArtist) } catch (e: Exception) {}
                }
                if (effectiveComposer.isNotEmpty()) {
                    try { tag.setField(FieldKey.COMPOSER, effectiveComposer) } catch (e: Exception) {}
                }
                if (effectiveYear > 0) {
                    try { tag.setField(FieldKey.YEAR, effectiveYear.toString()) } catch (e: Exception) {}
                }
                if (effectiveTrackNum > 0) {
                    try { tag.setField(FieldKey.TRACK, effectiveTrackNum.toString()) } catch (e: Exception) {}
                }
                if (effectiveDisc > 0) {
                    try { tag.setField(FieldKey.DISC_NO, effectiveDisc.toString()) } catch (e: Exception) {}
                }
                if (effectiveGenre.isNotEmpty()) {
                    try { tag.setField(FieldKey.GENRE, effectiveGenre) } catch (e: Exception) {}
                }
                if (effectiveIsrc.isNotEmpty()) {
                    try { tag.setField(FieldKey.ISRC, effectiveIsrc) } catch (e: Exception) {}
                }

                // ── Letras: LRC sincronizado o texto plano ──
                val lyricsToSave = syncedLrc.ifEmpty { plainLyrics }
                if (lyricsToSave.isNotEmpty()) {
                    try {
                        tag.setField(FieldKey.LYRICS, lyricsToSave)
                        Log.d(TAG, "Letras guardadas en ID3 (${if (syncedLrc.isNotEmpty()) "LRC sincronizado" else "texto plano"}) para: ${song.title}")
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo guardar letras en ID3: ${e.message}")
                    }
                }

                // ── Portada HD 1000x1000 ──
                try {
                    val coverMd5 = privateTrack?.albumCoverId?.takeIf { it.isNotEmpty() }
                    if (!coverMd5.isNullOrEmpty()) {
                        val coverUrl = "https://e-cdns-images.dzcdn.net/images/cover/$coverMd5/1000x1000-000000-80-0-0.jpg"
                        val coverReq = okhttp3.Request.Builder().url(coverUrl).get().build()
                        val coverResp = httpClient.newCall(coverReq).execute()
                        if (coverResp.isSuccessful) {
                            val coverBytes = coverResp.body?.bytes()
                            if (coverBytes != null && coverBytes.isNotEmpty()) {
                                val tempCover = File(outputFile.parentFile, ".cover_${song.id}.jpg")
                                FileOutputStream(tempCover).use { it.write(coverBytes) }
                                try {
                                    val artwork = org.jaudiotagger.tag.images.ArtworkFactory.createArtworkFromFile(tempCover)
                                    tag.deleteArtworkField()
                                    tag.setField(artwork)
                                    Log.d(TAG, "Portada HD 1000x1000 incrustada para: ${song.title}")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error incrustando portada HD: ${e.message}")
                                } finally {
                                    tempCover.delete()
                                }
                            }
                        }
                        coverResp.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo incrustar portada: ${e.message}")
                }

                audioFile.commit()
                Log.i(TAG, "Etiquetado ID3 completo: ISRC=$effectiveIsrc, Año=$effectiveYear, Género=$effectiveGenre, Letras=${lyricsToSave.isNotEmpty()}")

            } catch (e: Exception) {
                Log.e(TAG, "Error etiquetando archivo ID3: $e")
            }

            // 4. Enriquecimiento BPM / Camelot Key y persistencia en Room DB
            try {
                val entity = SongEntity(
                    playlistCreatorId = 0L,
                    id = song.id,
                    title = song.title,
                    trackNumber = song.trackNumber,
                    year = song.year,
                    duration = song.duration,
                    data = outputFile.absolutePath,
                    dateModified = System.currentTimeMillis(),
                    albumId = song.albumId,
                    albumName = song.albumName,
                    artistId = song.artistId,
                    artistName = song.artistName,
                    composer = song.composer,
                    albumArtist = song.albumArtist
                )

                val repository: RoomRepository? = try {
                    org.koin.java.KoinJavaComponent.get(RoomRepository::class.java)
                } catch (e: Exception) { null }

                val scannedEntity = BpmScanner.scanSongEntity(entity, repository)
                if (repository != null) {
                    try { repository.insertSongs(listOf(scannedEntity)) } catch (e: Exception) {
                        Log.w(TAG, "No se pudo insertar en Room DB: ${e.message}")
                    }
                }

                // Re-inyectar BPM y Key calculados al archivo ID3
                if (scannedEntity.bpm > 0f || scannedEntity.musicalKey.isNotEmpty()) {
                    try {
                        val af = AudioFileIO.read(outputFile)
                        val t = if (af is org.jaudiotagger.audio.mp3.MP3File) {
                            var t2 = af.tag as? org.jaudiotagger.tag.id3.AbstractID3v2Tag
                            if (t2 == null) { t2 = org.jaudiotagger.tag.id3.ID3v23Tag(); af.tag = t2 }
                            t2
                        } else af.tagOrCreateAndSetDefault
                        if (scannedEntity.bpm > 0f) t.setField(FieldKey.BPM, scannedEntity.bpm.toInt().toString())
                        if (scannedEntity.musicalKey.isNotEmpty()) t.setField(FieldKey.KEY, scannedEntity.musicalKey)
                        try { t.setField(FieldKey.COMMENT, "REPLAYGAIN_TRACK_GAIN=${scannedEntity.replayGain} dB") } catch (ignore: Exception) {}
                        af.commit()
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo inyectar BPM/Key en ID3: ${e.message}")
                    }
                }
                Log.i(TAG, "AutoMix enrichment done: BPM=${scannedEntity.bpm}, Key=${scannedEntity.musicalKey}, CueOut=${scannedEntity.cueOutMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error en enriquecimiento Room DB: $e")
            }
        }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
    }
}


