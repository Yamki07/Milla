/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.util.Log
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Motor de desencriptación nativo en Kotlin para streams de audio de Deezer (FLAC/MP3).
 * Reemplaza la lógica de StreamServer/DeezerDecryptor basada en socket local por un
 * descifrado al vuelo en memoria.
 *
 * Algoritmo:
 * - Cifrado: Blowfish/CBC/NoPadding
 * - Vector de inicialización (IV): estático 8 bytes [0, 1, 2, 3, 4, 5, 6, 7]
 * - Llave (Key): Derivada mediante MD5 del ID de pista (32 hex chars) combinada con XOR
 *   en 16 iteraciones sobre el secreto maestro de Deezer ("g4el58wc0zvf9na1").
 */
object DeezerDecryptor {
    private const val TAG = "DeezerDecryptor"

    // Secreto maestro de Deezer extraído del protocolo oficial
    private const val SECRET = "g4el58wc0zvf9na1"

    // Vector de Inicialización estático (8 bytes en CBC para Blowfish)
    private val IV = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)

    // Transformación criptográfica utilizada para los bloques de 2048 bytes
    private const val BLOWFISH_TRANSFORMATION = "Blowfish/CBC/NoPadding"

    /**
     * Deriva la llave de 16 bytes para una pista específica de Deezer a partir de su [trackId].
     */
    fun getKey(id: String): ByteArray {
        return try {
            val md5 = MessageDigest.getInstance("MD5")
            val md5id = md5.digest(id.toByteArray(Charsets.UTF_8))
            val idmd5 = bytesToHex(md5id).lowercase()
            val key = ByteArray(16)

            // Lógica XOR de 16 iteraciones
            for (i in 0 until 16) {
                val s0 = idmd5[i].code
                val s1 = idmd5[i + 16].code
                val s2 = SECRET[i].code
                key[i] = (s0 xor s1 xor s2).toByte()
            }
            key
        } catch (e: Exception) {
            Log.e(TAG, "Error generando llave para trackId=$id: $e")
            ByteArray(0)
        }
    }

    /**
     * Desencripta un bloque (chunk) de 2048 bytes utilizando la llave Blowfish de la pista.
     */
    fun decryptChunk(key: ByteArray, data: ByteArray): ByteArray {
        return try {
            val sKey = SecretKeySpec(key, "Blowfish")
            val cipher = Cipher.getInstance(BLOWFISH_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, sKey, IvParameterSpec(IV))
            cipher.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error desencriptando bloque Blowfish: $e")
            ByteArray(0)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
