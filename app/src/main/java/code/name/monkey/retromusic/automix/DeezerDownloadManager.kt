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
            // 1. Etiquetado físico inicial de ID3 con JAudioTagger
            try {
                val audioFile = AudioFileIO.read(outputFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.TITLE, song.title)
                tag.setField(FieldKey.ARTIST, song.artistName)
                tag.setField(FieldKey.ALBUM, song.albumName)
                if (song.year > 0) {
                    tag.setField(FieldKey.YEAR, song.year.toString())
                }
                tag.setField(FieldKey.GENRE, "Milla Automix")

                // Obtener datos privados de Deezer para la carátula y letras
                try {
                    var lyrics = code.name.monkey.retromusic.util.AmllLyricsFetcher.fetchLyrics(song.title, song.artistName)
                    if (lyrics == null || lyrics.isEmpty()) {
                        lyrics = code.name.monkey.retromusic.util.MusixmatchFetcher.getEnhancedLrc(song.title, song.artistName)
                    }
                    if (lyrics == null || lyrics.isEmpty()) {
                        lyrics = code.name.monkey.retromusic.util.LRCLibFetcher.fetchLyrics(song)
                    }
                    if (lyrics == null || lyrics.isEmpty()) {
                        lyrics = DeezerApiClient.getLyrics(song.id.toString())
                    }
                    if (!lyrics.isNullOrEmpty()) {
                        tag.setField(FieldKey.LYRICS, lyrics)
                        Log.d(TAG, "Letras guardadas en archivo ID3 para: ${song.title}")
                    }

                    val privateTrack = DeezerApiClient.fetchPrivateTrackData(song.id.toString())
                    val coverMd5 = privateTrack?.albumCoverId
                    if (!coverMd5.isNullOrEmpty()) {
                        val coverUrl = "https://e-cdns-images.dzcdn.net/images/cover/$coverMd5/1000x1000-000000-80-0-0.jpg"
                        val coverRequest = Request.Builder().url(coverUrl).get().build()
                        val coverResponse = httpClient.newCall(coverRequest).execute()
                        if (coverResponse.isSuccessful) {
                            val coverBody = coverResponse.body
                            if (coverBody != null) {
                                val tempCoverFile = File(outputFile.parentFile, ".cover_${song.id}.jpg")
                                FileOutputStream(tempCoverFile).use { out ->
                                    out.write(coverBody.bytes())
                                }
                                
                                try {
                                    val artwork = AndroidArtwork.createArtworkFromFile(tempCoverFile)
                                    tag.deleteArtworkField()
                                    tag.setField(artwork)
                                } catch(e: Exception) {
                                    Log.e(TAG, "Error incrustando caratula en ID3: ${e.message}")
                                }
                                
                                tempCoverFile.delete()
                            }
                        }
                        coverResponse.close()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo etiquetar carátula o letras: ${e.message}")
                }
                audioFile.commit()
                Log.i(TAG, "Etiquetado físico ID3 completo para: ${outputFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error etiquetando archivo ID3: $e")
            }

            // 2. Enriquecimiento RMS / Camelot Key / BPM y guardado en Room DB
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
                } catch (e: Exception) {
                    null
                }

                val scannedEntity = BpmScanner.scanSongEntity(entity, repository)
                if (repository != null) {
                    try {
                        repository.insertSongs(listOf(scannedEntity))
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo insertar canción en Room DB: ${e.message}")
                    }
                }
                Log.i(
                    TAG,
                    "Enriquecimiento Automix y Persistencia en Room completados: BPM=${scannedEntity.bpm}, Key=${scannedEntity.musicalKey}, ReplayGain=${scannedEntity.replayGain}dB, CueOut=${scannedEntity.cueOutMs}ms"
                )


                // Re-inyectar BPM y Key calculados de vuelta al archivo ID3
                try {
                    val audioFile = AudioFileIO.read(outputFile)
                    val tag = audioFile.tagOrCreateAndSetDefault
                    if (scannedEntity.bpm > 0f) {
                        tag.setField(FieldKey.BPM, scannedEntity.bpm.toInt().toString())
                    }
                    if (scannedEntity.musicalKey.isNotEmpty()) {
                        tag.setField(FieldKey.KEY, scannedEntity.musicalKey)
                    }
                    try {
                        tag.setField(FieldKey.COMMENT, "REPLAYGAIN_TRACK_GAIN=${scannedEntity.replayGain} dB")
                    } catch (ignore: Exception) {}
                    audioFile.commit()
                } catch (e: Exception) {

                    Log.w(TAG, "No se pudo inyectar BPM/Key en ID3: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en enriquecimiento RMS y Room DB: $e")
            }
        }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
    }
}

