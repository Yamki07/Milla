package code.name.monkey.retromusic.automix

import android.content.Context
import android.util.Log
import code.name.monkey.retromusic.repository.RealSongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Escanea toda la biblioteca local de forma manual a petición del usuario.
 * Cada pista se analiza y sincroniza mediante el upsert idempotente de BpmScanner.
 * Nunca borra el catálogo remoto compartido antes de empezar.
 */
object LocalMetadataScanner {

    private const val TAG = "LocalMetadataScanner"

    suspend fun scanEntireDeviceAndUpload(context: Context, onProgress: (Int, Int, String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "Iniciando escáner total del dispositivo...")
            // 1. Obtener todas las canciones locales
            val songs = RealSongRepository(context).songs()
            val total = songs.size
            if (total == 0) {
                Log.w(TAG, "No hay canciones locales para escanear.")
                return@withContext
            }

            Log.i(TAG, "Escaneando $total canciones desde el almacenamiento local.")
            
            // 2. Analizar y subir una por una. BpmScanner usa generateTrackId y
            // SupabaseClientManager.uploadMetadata con resolution=merge-duplicates.
            var count = 0
            for (song in songs) {
                try {
                    // scanSong ya llama a SupabaseClientManager.uploadMetadata internamente!
                    BpmScanner.scanSong(song)
                } catch (e: Exception) {
                    Log.e(TAG, "Error escaneando canción ${song.title}: ${e.message}")
                }
                count++
                withContext(Dispatchers.Main) {
                    onProgress(count, total, song.title)
                }
            }
            Log.i(TAG, "Escaneo y subida completados exitosamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error general en scanEntireDeviceAndUpload: ${e.message}")
        }
    }
}
