/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import kotlin.math.min

/**
 * Fuente de datos nativa para ExoPlayer ([DataSource]) que intercepta un stream de Deezer en memoria,
 * acumula paquetes en bloques de 2048 bytes y desencripta al vuelo 1 de cada 3 bloques
 * utilizando [DeezerDecryptor].
 *
 * Elimina la necesidad de servidores locales proxy o sockets (como StreamServer / NanoHTTPD),
 * logrando latencia cero y menor consumo energético.
 */
class DeezerDataSource(
    private val upstream: DataSource,
    private val trackId: String
) : DataSource {

    private val trackKey: ByteArray = DeezerDecryptor.getKey(trackId)
    private var chunkCounter: Int = 0
    private var dropBytes: Int = 0
    private var bytesRemaining: Long = 0L

    // Búfer interno para acumular exactamente 2048 bytes por bloque
    private val chunkBuffer = ByteArray(2048)
    private var chunkBufferPosition: Int = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        // Calcular los offsets alineados a bloques de 2048 bytes para Deezer
        val startBytes = dataSpec.position
        val deezerStart = startBytes - (startBytes % 2048)
        dropBytes = (startBytes % 2048).toInt()

        chunkCounter = (deezerStart / 2048).toInt()

        // Ajustamos la petición al servidor upstream para que comience en un límite de bloque 2048
        val adjustedDataSpec = dataSpec.buildUpon()
            .setPosition(deezerStart)
            .build()

        bytesRemaining = upstream.open(adjustedDataSpec)

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= dropBytes
        }
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        // Llenar nuestro búfer de 2048 bytes
        while (chunkBufferPosition < 2048) {
            val read = upstream.read(chunkBuffer, chunkBufferPosition, 2048 - chunkBufferPosition)
            if (read == C.RESULT_END_OF_INPUT) {
                if (chunkBufferPosition == 0) return C.RESULT_END_OF_INPUT
                break // Fin de archivo, tenemos un bloque parcial
            }
            chunkBufferPosition += read
        }

        // Si tenemos un bloque completo de 2048 bytes
        var dataToCopy = chunkBuffer
        var availableDataLen = chunkBufferPosition

        if (chunkBufferPosition == 2048) {
            // Desencriptar 1 de cada 3 bloques
            if (chunkCounter % 3 == 0) {
                dataToCopy = DeezerDecryptor.decryptChunk(trackKey, chunkBuffer)
            }
            chunkCounter++
            chunkBufferPosition = 0 // Resetear para el próximo ciclo
        }

        // Soltar los bytes iniciales si hicimos un seek (adelantar canción en medio del bloque)
        var dataOffset = 0
        if (dropBytes > 0) {
            dataOffset = dropBytes
            availableDataLen -= dropBytes
            dropBytes = 0
        }

        // Copiar los datos desencriptados al buffer de ExoPlayer
        val bytesToCopy = min(length, availableDataLen)
        System.arraycopy(dataToCopy, dataOffset, buffer, offset, bytesToCopy)

        // Si no copiamos todo, mover el remanente al inicio del búfer (caso de lectura corta)
        if (bytesToCopy < availableDataLen && chunkBufferPosition == 0) {
            System.arraycopy(dataToCopy, dataOffset + bytesToCopy, chunkBuffer, 0, availableDataLen - bytesToCopy)
            chunkBufferPosition = availableDataLen - bytesToCopy
            chunkCounter-- // Revertir contador porque no hemos consumido este bloque totalmente
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesToCopy
        }

        return bytesToCopy
    }

    override fun getUri(): Uri? = upstream.uri

    override fun close() {
        upstream.close()
    }
}
