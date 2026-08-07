package code.name.monkey.retromusic.automix

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import okhttp3.Request
import okhttp3.OkHttpClient

class DeezerDownloadTest {

    @Test
    fun testDownloadAndDecrypt() = runBlocking {
        println("--- INICIANDO PRUEBA DE DESCARGA DEEZER (FLAC) ---")
        
        val trackId = "3135556" // Daft Punk
        val quality = 9 // FLAC
        
        // 1. Obtener URL
        val url = DeezerApiClient.getStreamUrl(
            DeezerTrack(id = trackId, title = "Test", artistName = "Test", albumTitle = "Test", albumCoverId = "", durationSec = 0, explicit = false, md5Origin = "", mediaVersion = "", trackToken = "", fileSize320 = 0L, fileSize128 = 0L, fileFlac = 0L),
            "FLAC"
        )
        
        if (url == null) {
            println("❌ Error: No se pudo obtener la URL del track.")
            return@runBlocking
        }
        
        println("✅ URL obtenida con éxito: $url")
        
        // 2. Descargar y desencriptar
        val outputFile = File("D:\\Descargas (S)\\Milla\\descargas_prueba", "daft_punk_test.flac")
        outputFile.parentFile.mkdirs()
        
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            println("❌ Error: Respuesta HTTP no exitosa ${response.code}")
            return@runBlocking
        }
        
        val body = response.body
        if (body == null) {
            println("❌ Error: Cuerpo de la respuesta vacío.")
            return@runBlocking
        }
        
        println("⬇️ Iniciando descarga y desencriptación Blowfish...")
        val trackKey = DeezerDecryptor.getKey(trackId)
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
                    buffer
                }
                
                outputStream.write(dataToWrite, 0, bytesReadInChunk)
                bytesDownloaded += bytesReadInChunk
                chunkIndex++
                
                if (bytesReadInChunk < 2048) break
            }
            outputStream.flush()
        }
        
        println("✅ Descarga y desencriptación completada: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
    }
}
