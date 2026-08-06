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

    fun downloadTrack(context: Context, song: Song, tidalTrackId: String, tidalCoverUrl: String) {
        if (activeJobs.containsKey(tidalTrackId)) return

        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                _downloadState.value = DownloadState.Downloading(tidalTrackId, 0)
                Log.d(TAG, "Iniciando descarga de Tidal para: ${song.title} ($tidalTrackId)")

                val streamUrl = TidalApiClient.getStreamUrl(tidalTrackId)
                if (streamUrl.isNullOrEmpty()) {
                    _downloadState.value = DownloadState.Error(tidalTrackId, "No se pudo obtener el enlace de Tidal")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Tidal Error: Token expirado o URL no encontrada", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val downloadDir = DeezerDownloadManager.getDownloadsDirectory(context)
                if (!downloadDir.exists()) downloadDir.mkdirs()

                // Tidal Lossless usually returns FLAC (sometimes inside MP4, but Jaudiotagger handles it or we save it directly)
                val fileName = "${sanitizeFileName(song.artistName)} - ${sanitizeFileName(song.title)}.flac"
                var outputFile = File(downloadDir, fileName)

                val success = downloadDirect(streamUrl, outputFile, tidalTrackId)
                if (success) {
                    outputFile = ensureCorrectExtension(outputFile)
                    _downloadState.value = DownloadState.PostProcessing(tidalTrackId)
                    
                    // Obtener letras de Tidal
                    val lyrics = TidalApiClient.getLyrics(tidalTrackId)
                    
                    tagAndEnrichDownloadedFile(context, outputFile, song, tidalCoverUrl, lyrics)
                    _downloadState.value = DownloadState.Completed(tidalTrackId, outputFile.absolutePath, song)
                    Log.i(TAG, "Descarga Tidal completada: ${outputFile.absolutePath}")
                } else {
                    if (outputFile.exists()) outputFile.delete()
                    _downloadState.value = DownloadState.Error(tidalTrackId, "Fallo al descargar archivo Tidal")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Tidal Error: Descarga falló", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en descarga de Tidal=$tidalTrackId: $e")
                _downloadState.value = DownloadState.Error(tidalTrackId, e.message ?: "Error desconocido")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Tidal Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                activeJobs.remove(tidalTrackId)
            }
        }
        activeJobs[tidalTrackId] = job
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
                            val progress = ((bytesDownloaded * 100) / contentLength).toInt().coerceIn(0, 99)
                            _downloadState.value = DownloadState.Downloading(trackId, progress)
                        }
                    }
                    outputStream.flush()
                }
                response.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error durante Tidal downloadDirect: $e")
                false
            }
        }

    private suspend fun tagAndEnrichDownloadedFile(
        context: Context, 
        outputFile: File, 
        song: Song, 
        tidalCoverUrl: String, 
        lyrics: String
    ) = withContext(Dispatchers.IO) {
            try {
                val audioFile = AudioFileIO.read(outputFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.TITLE, song.title)
                tag.setField(FieldKey.ARTIST, song.artistName)
                tag.setField(FieldKey.ALBUM, song.albumName)
                if (song.year > 0) {
                    tag.setField(FieldKey.YEAR, song.year.toString())
                }
                tag.setField(FieldKey.GENRE, "Milla Automix (Tidal)")
                
                if (lyrics.isNotEmpty()) {
                    try {
                        tag.setField(FieldKey.LYRICS, lyrics)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error inyectando letras: $e")
                    }
                }

                if (tidalCoverUrl.isNotEmpty()) {
                    try {
                        val coverRequest = Request.Builder().url(tidalCoverUrl).get().build()
                        val coverResponse = httpClient.newCall(coverRequest).execute()
                        if (coverResponse.isSuccessful) {
                            val coverBody = coverResponse.body
                            if (coverBody != null) {
                                val tempCoverFile = File(outputFile.parentFile, ".cover_tidal_${song.id}.jpg")
                                FileOutputStream(tempCoverFile).use { out ->
                                    out.write(coverBody.bytes())
                                }
                                val artwork = AndroidArtwork.createArtworkFromFile(tempCoverFile)
                                tag.deleteArtworkField()
                                tag.setField(artwork)
                                tempCoverFile.delete()
                            }
                        }
                        coverResponse.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo etiquetar carátula Tidal: ${e.message}")
                    }
                }
                audioFile.commit()
                Log.i(TAG, "Etiquetado físico ID3 (Tidal) completo para: ${outputFile.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error etiquetando archivo ID3 Tidal: $e")
            }

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
                    try {
                        repository.insertSongs(listOf(scannedEntity))
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo insertar canción Tidal en Room DB: ${e.message}")
                    }
                }

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
                    Log.w(TAG, "No se pudo inyectar BPM/Key Tidal en ID3: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en enriquecimiento RMS y Room DB Tidal: $e")
            }
        }

    private fun ensureCorrectExtension(file: File): File {
        try {
            val bytes = ByteArray(8)
            java.io.FileInputStream(file).use { it.read(bytes) }
            val isMp4 = bytes.size >= 8 && bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() && bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()
            if (isMp4 && file.name.endsWith(".flac")) {
                val newFile = File(file.parent, file.name.replace(".flac", ".m4a"))
                if (file.renameTo(newFile)) {
                    Log.i(TAG, "Formato MP4 detectado, renombrado a .m4a: ${newFile.name}")
                    return newFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comprobando extensión del archivo: $e")
        }
        return file
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
    }
}
