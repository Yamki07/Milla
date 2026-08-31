package code.name.monkey.retromusic.automix

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Analizador PCM deliberadamente ultra-lite. Solo calcula silencios de entrada/salida,
 * nivel integrado aproximado y pico. BPM y tonalidad se reservan para el backend verificado.
 */
object PcmAudioAnalyzer {
    private const val TIMEOUT_US = 10_000L

    data class EnergyWindow(val positionMs: Long, val rms: Float)

    data class Result(
        val bpm: Float = 0f,
        val bpmConfidence: Float = 0f,
        val musicalKey: String = "",
        val camelotKey: String = "",
        val cueInMs: Long,
        val cueOutMs: Long,
        val introSilenceMs: Long,
        val outroSilenceMs: Long,
        val integratedLufsApprox: Float,
        val truePeak: Float,
        val energyWindows: List<EnergyWindow>
    )

    fun analyze(context: Context, sourceUri: String, isStopped: () -> Boolean): Result {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, sourceUri.toInputUri(), null)
            val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("No se encontró una pista de audio decodificable")
            extractor.selectTrack(audioTrack)
            val inputFormat = extractor.getTrackFormat(audioTrack)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("El formato de audio no declara MIME")
            decoder = MediaCodec.createDecoderByType(mime).also { codec ->
                codec.configure(inputFormat, null, null, 0)
                codec.start()
            }

            val collector = AmplitudeCollector()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var channels = inputFormat.integerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            while (!outputEos) {
                if (isStopped()) throw InterruptedException("Análisis cancelado por WorkManager")
                if (!inputEos) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = decoder.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("InputBuffer nulo")
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.outputFormat
                        channels = format.integerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, channels).coerceAtLeast(1)
                        encoding = format.integerOrDefault(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        if (bufferInfo.size > 0) decoder.getOutputBuffer(outputIndex)?.let {
                            collector.consume(it, bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs / 1_000L, channels, encoding)
                        }
                        outputEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            return collector.toResult()
        } finally {
            runCatching { extractor.release() }
            runCatching { decoder?.release() }
        }
    }

    private fun String.toInputUri(): Uri = if (contains("://")) Uri.parse(this) else Uri.fromFile(File(this))
    private fun MediaFormat.integerOrDefault(key: String, defaultValue: Int): Int = if (containsKey(key)) getInteger(key) else defaultValue

    private class AmplitudeCollector {
        private val windows = mutableListOf<EnergyWindow>()
        private var peak = 0f
        private var sumSquares = 0.0
        private var samples = 0L

        fun consume(output: ByteBuffer, offset: Int, size: Int, timeMs: Long, channels: Int, encoding: Int) {
            output.order(ByteOrder.LITTLE_ENDIAN)
            output.position(offset)
            output.limit(offset + size)
            var blockSquares = 0.0
            var blockSamples = 0
            if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                val values = output.slice().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                while (values.hasRemaining()) {
                    val value = values.get().coerceIn(-1f, 1f)
                    blockSquares += value * value; peak = max(peak, abs(value)); blockSamples++
                }
            } else {
                val values = output.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                while (values.hasRemaining()) {
                    val value = values.get().toFloat() / Short.MAX_VALUE
                    blockSquares += value * value; peak = max(peak, abs(value)); blockSamples++
                }
            }
            if (blockSamples > 0) {
                windows += EnergyWindow(timeMs, sqrt(blockSquares / blockSamples).toFloat())
                sumSquares += blockSquares; samples += blockSamples
            }
        }

        fun toResult(): Result {
            if (windows.isEmpty() || samples == 0L) return Result(cueInMs = 0L, cueOutMs = 0L, introSilenceMs = 0L, outroSilenceMs = 0L, integratedLufsApprox = -70f, truePeak = 0f, energyWindows = emptyList())
            val rms = sqrt(sumSquares / samples).toFloat()
            val maxEnergy = windows.maxOf { it.rms }
            val threshold = max(0.0005f, maxEnergy * 0.06f)
            val cueIn = windows.firstOrNull { it.rms >= threshold }?.positionMs ?: 0L
            val cueOut = windows.lastOrNull { it.rms >= threshold }?.positionMs ?: 0L
            val duration = windows.last().positionMs
            return Result(
                cueInMs = cueIn,
                cueOutMs = cueOut,
                introSilenceMs = cueIn,
                outroSilenceMs = (duration - cueOut).coerceAtLeast(0L),
                integratedLufsApprox = if (rms > 0f) 20f * log10(rms) else -70f,
                truePeak = peak,
                energyWindows = windows
            )
        }
    }
}
