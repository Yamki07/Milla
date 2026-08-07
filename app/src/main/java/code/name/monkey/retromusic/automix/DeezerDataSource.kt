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

    // Búfer interno para acumular exactamente 2048 bytes por bloque (encriptados)
    private val chunkBuffer = ByteArray(2048)
    private var chunkBufferPosition: Int = 0

    // Búfer para almacenar el bloque procesado (desencriptado o plano) del que se lee poco a poco
    private var processedBuffer: ByteArray = ByteArray(0)
    private var processedBufferOffset: Int = 0
    private var processedBufferLength: Int = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        // Calcular los offsets alineados a bloques de 2048 bytes para Deezer
        val startBytes = dataSpec.position
        val deezerStart = startBytes - (startBytes % 2048)
        val initialDropBytes = (startBytes % 2048).toInt()

        chunkCounter = (deezerStart / 2048).toInt()
        chunkBufferPosition = 0
        processedBufferLength = 0

        // Ajustamos la petición al servidor upstream para que comience en un límite de bloque 2048
        val adjustedDataSpec = dataSpec.buildUpon()
            .setPosition(deezerStart)
            .build()

        bytesRemaining = upstream.open(adjustedDataSpec)

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= initialDropBytes
        }

        // Si hay que saltar bytes iniciales, los marcamos en el offset procesado
        if (initialDropBytes > 0) {
            // Forzamos la lectura y desencriptación del primer bloque, y ajustamos el offset
            readNextChunk()
            processedBufferOffset = initialDropBytes
            processedBufferLength -= initialDropBytes
        }

        return bytesRemaining
    }

    private fun readNextChunk(): Boolean {
        chunkBufferPosition = 0
        while (chunkBufferPosition < 2048) {
            val read = upstream.read(chunkBuffer, chunkBufferPosition, 2048 - chunkBufferPosition)
            if (read == C.RESULT_END_OF_INPUT) {
                if (chunkBufferPosition == 0) return false
                break // Fin de archivo, tenemos un bloque parcial
            }
            chunkBufferPosition += read
        }

        if (chunkBufferPosition == 2048 && chunkCounter % 3 == 0) {
            processedBuffer = DeezerDecryptor.decryptChunk(trackKey, chunkBuffer)
        } else {
            // Copiamos para no mutar el buffer interno mientras se lee el próximo
            processedBuffer = chunkBuffer.copyOf(chunkBufferPosition)
        }
        processedBufferOffset = 0
        processedBufferLength = chunkBufferPosition
        chunkCounter++
        return true
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        if (processedBufferLength == 0) {
            val hasMore = readNextChunk()
            if (!hasMore) return C.RESULT_END_OF_INPUT
        }

        val bytesToCopy = min(length, processedBufferLength)
        System.arraycopy(processedBuffer, processedBufferOffset, buffer, offset, bytesToCopy)

        processedBufferOffset += bytesToCopy
        processedBufferLength -= bytesToCopy

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
