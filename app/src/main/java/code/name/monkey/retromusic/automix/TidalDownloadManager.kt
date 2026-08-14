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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.repository.RoomRepository
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor moderno de descargas nativo para Tidal con Corrutinas y Flow.
 * Reemplaza a TidalDownloadManager, conectando con TidalApiClient para obtener FLAC/MP3 directos
 * y guardado en almacenamiento físico para uso offline en Automix.
 */
object TidalDownloadManager {
    private const val TAG = "TidalDownloadManager"

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
     * Alias para compatibilidad con la UI de Milla.
     */
    fun downloadTrack(context: Context, song: Song, quality: Int = 9) {
        startDownload(context, song, quality)
    }

    /**
     * Inicia la descarga de una canción [song] desde Tidal.
     * @param quality 9 para FLAC, 3 para MP3 320kbps. (Tidal usa lossless principal, MP3 como fallback)
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
                Log.d(TAG, "Iniciando descarga de Tidal para: ${song.title} ($trackId)")

                // El trackId en Tidal es la URL interna "tidal://track/12345::cover" en la clase Song
                // Extraemos el ID real si viene formateado así:
                val realTrackId = if (song.data.startsWith("tidal://track/")) {
                    song.data.removePrefix("tidal://track/").substringBefore("::")
                } else {
                    trackId
                }

                val coverId = if (song.data.contains("::")) {
                    song.data.substringAfter("::")
                } else {
                    ""
                }

                val streamUrl = TidalApiClient.getStreamUrl(realTrackId)

                if (streamUrl.isNullOrEmpty()) {
                    _downloadState.value = DownloadState.Error(trackId, "No se pudo obtener el enlace de Tidal")
                    withContext(Dispatchers.Main) {
                        val errorMsg = TidalApiClient.lastError ?: "Token expirado o URL no encontrada"
                        android.widget.Toast.makeText(context, "Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val downloadDir = getDownloadsDirectory(context)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                // Identify actual format from URL
                val isFlac = streamUrl.contains(".flac") || streamUrl.contains("audio/flac")
                val isMp4 = streamUrl.contains(".mp4") || streamUrl.contains("audio/mp4")
                val extension = if (isFlac) "flac" else if (isMp4) "m4a" else "mp3"
                val fileName = "${sanitizeFileName(song.artistName)} - ${sanitizeFileName(song.title)}.$extension"
                val outputFile = File(downloadDir, fileName)

                val success = downloadDirect(streamUrl, outputFile, realTrackId)
                if (success) {
                    _downloadState.value = DownloadState.PostProcessing(trackId)
                    tagAndEnrichDownloadedFile(context, outputFile, song, realTrackId, coverId)
                    val mimeType = when (extension) {
                        "flac" -> "audio/flac"
                        "m4a" -> "audio/mp4"
                        else -> "audio/mpeg"
                    }
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf(mimeType)) { path, uri ->
                        Log.i(TAG, "MediaScanner completado para: $path, uri=$uri")
                    }
                    _downloadState.value = DownloadState.Completed(trackId, outputFile.absolutePath, song)
                    Log.i(TAG, "Descarga completada: ${outputFile.absolutePath}")

                } else {
                    if (outputFile.exists()) outputFile.delete()
                    _downloadState.value = DownloadState.Error(trackId, "Fallo al descargar el archivo de Tidal")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error: Descarga falló", android.widget.Toast.LENGTH_LONG).show()
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

    fun cancelDownload(trackId: String) {
        activeJobs[trackId]?.cancel()
        activeJobs.remove(trackId)
        _downloadState.value = DownloadState.Idle
    }

    fun getDownloadsDirectory(context: Context): File {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        return File(baseDir, "RetroMusic")
    }

    private suspend fun downloadDirect(url: String, outputFile: File, trackId: String): Boolean =
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
                val inputStream = body.byteStream()
                
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesDownloaded = 0L

                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        
                        outputStream.write(buffer, 0, read)
                        bytesDownloaded += read
                        
                        if (contentLength > 0) {
                            val percent = ((bytesDownloaded * 100) / contentLength).toInt()
                            if (percent % 5 == 0) {
                                _downloadState.value = DownloadState.Downloading(trackId, percent)
                            }
                        }
                    }
                    
                    _downloadState.value = DownloadState.Downloading(trackId, 100)
                    outputStream.flush()
                }
                response.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error durante downloadDirect: $e")
                false
            }
        }

    private suspend fun tagAndEnrichDownloadedFile(context: Context, outputFile: File, song: Song, realTrackId: String, coverId: String) =
        withContext(Dispatchers.IO) {
            // 1. Obtener letras (Tidal) o Fallback
            var lyricsToSave = ""
            try {
                lyricsToSave = TidalApiClient.getLyrics(realTrackId)
                if (lyricsToSave.isEmpty()) {
                    val lrcFromAmll = code.name.monkey.retromusic.util.AmllLyricsFetcher.fetchLyrics(song.title, song.artistName) ?: ""
                    val lrcFromMusixmatch = if (lrcFromAmll.isEmpty()) {
                        code.name.monkey.retromusic.util.MusixmatchFetcher.getEnhancedLrc(song.title, song.artistName) ?: ""
                    } else ""
                    val lrcFromLib = if (lrcFromAmll.isEmpty() && lrcFromMusixmatch.isEmpty()) {
                        code.name.monkey.retromusic.util.LRCLibFetcher.fetchLyrics(song) ?: ""
                    } else ""
                    lyricsToSave = lrcFromAmll.ifEmpty { lrcFromMusixmatch.ifEmpty { lrcFromLib } }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error obteniendo letras: ${e.message}")
            }

            // 2. Etiquetado ID3 con JAudioTagger
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
                    } catch (e2: Exception) {}
                }

                if (audioFile == null) return@withContext

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

                tag.setField(FieldKey.TITLE, song.title)
                tag.setField(FieldKey.ARTIST, song.artistName)
                tag.setField(FieldKey.ALBUM, song.albumName)
                
                if (lyricsToSave.isNotEmpty()) {
                    try {
                        tag.setField(FieldKey.LYRICS, lyricsToSave)
                    } catch (e: Exception) { }
                }

                // Portada HD
                if (coverId.isNotEmpty()) {
                    try {
                        val coverUrl = "https://resources.tidal.com/images/${coverId.replace("-", "/")}/1280x1280.jpg"
                        val coverReq = okhttp3.Request.Builder().url(coverUrl).get().build()
                        val coverResp = httpClient.newCall(coverReq).execute()
                        if (coverResp.isSuccessful) {
                            val coverBytes = coverResp.body?.bytes()
                            if (coverBytes != null && coverBytes.isNotEmpty()) {
                                val tempCover = File(outputFile.parentFile, ".cover_${realTrackId}.jpg")
                                FileOutputStream(tempCover).use { it.write(coverBytes) }
                                try {
                                    val artwork = org.jaudiotagger.tag.images.ArtworkFactory.createArtworkFromFile(tempCover)
                                    tag.deleteArtworkField()
                                    tag.setField(artwork)
                                } finally {
                                    tempCover.delete()
                                }
                            }
                        }
                        coverResp.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo incrustar portada: ${e.message}")
                    }
                }

                audioFile.commit()
            } catch (e: Exception) {
                Log.e(TAG, "Error etiquetando archivo ID3: $e")
            }

            // 3. Registrar y encolar análisis PCM ultra-lite. No se infieren BPM ni tonalidad localmente.
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

                if (repository != null) {
                    try { repository.insertSongs(listOf(entity)) } catch (e: Exception) { }
                }
                code.name.monkey.retromusic.workers.TrackAnalysisWorker.enqueue(
                    context = context,
                    sourceUri = outputFile.toURI().toString(),
                    title = song.title,
                    artist = song.artistName,
                    sourceType = "tidal_download",
                    legacySongId = entity.id
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error en enriquecimiento Room DB: $e")
            }
        }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
    }
}
