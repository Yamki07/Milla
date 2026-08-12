package code.name.monkey.retromusic.automix

import android.content.Context
import android.util.Log
import code.name.monkey.retromusic.network.SupabaseClientManager
import code.name.monkey.retromusic.repository.RealSongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Escanea toda la biblioteca local de forma manual a petición del usuario.
 * Destruye la base de datos de Supabase y sube datos completamente reales leídos de los archivos.
 */
object LocalMetadataScanner {

    private const val TAG = "LocalMetadataScanner"

    suspend fun scanEntireDeviceAndUpload(context: Context, onProgress: (Int, Int, String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "Iniciando escáner total del dispositivo...")
            // 1. Limpiar base de datos
            val cleared = SupabaseClientManager.clearAllData()
            if (!cleared) {
                Log.e(TAG, "No se pudo limpiar la base de datos en Supabase, abortando escaneo.")
                return@withContext
            }
            
            // 2. Obtener todas las canciones locales
            val songs = RealSongRepository(context).songs()
            val total = songs.size
            if (total == 0) {
                Log.w(TAG, "No hay canciones locales para escanear.")
                return@withContext
            }

            Log.i(TAG, "Escaneando $total canciones desde el almacenamiento local.")
            
            // 3. Analizar y subir una por una
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
