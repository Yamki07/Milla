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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Analizador inicial basado en PCM. Conserva los archivos originales y entrega
 * datos de baja complejidad para Room; la detección tonal/estructural avanzada
 * se añadirá en una fase posterior sobre este mismo contrato.
 */
object PcmAudioAnalyzer {
    private const val TIMEOUT_US = 10_000L
    private const val MIN_BPM = 70f
    private const val MAX_BPM = 190f

    data class EnergyWindow(val positionMs: Long, val rms: Float)

    data class Result(
        val bpm: Float,
        val bpmConfidence: Float,
        val cueInMs: Long,
        val cueOutMs: Long,
        val introSilenceMs: Long,
        val outroSilenceMs: Long,
        val integratedLufsApprox: Float,
        val truePeak: Float,
        val beatPositionsMs: List<Long>,
        val energyWindows: List<EnergyWindow>
    )

    fun analyze(context: Context, sourceUri: String, isStopped: () -> Boolean): Result {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            val uri = sourceUri.toInputUri()
            extractor.setDataSource(context, uri, null)
            val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("No se encontró una pista de audio decodificable")

            extractor.selectTrack(audioTrack)
            val inputFormat = extractor.getTrackFormat(audioTrack)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("El formato de audio no declara MIME")
            decoder = MediaCodec.createDecoderByType(mime)
            val codec = decoder ?: throw IllegalStateException("No se pudo crear el decoder de audio")
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val collector = EnergyCollector()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var outputSampleRate = inputFormat.integerOrDefault(MediaFormat.KEY_SAMPLE_RATE, 44_100)
            var outputChannels = inputFormat.integerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            while (!outputEos) {
                if (isStopped()) throw InterruptedException("Análisis cancelado por WorkManager")
                if (!inputEos) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IllegalStateException("InputBuffer nulo")
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        outputSampleRate = outputFormat.integerOrDefault(MediaFormat.KEY_SAMPLE_RATE, outputSampleRate)
                        outputChannels = outputFormat.integerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, outputChannels).coerceAtLeast(1)
                        pcmEncoding = outputFormat.integerOrDefault(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER,
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        if (bufferInfo.size > 0) {
                            codec.getOutputBuffer(outputIndex)?.let { output ->
                                collector.consume(
                                    output = output,
                                    offset = bufferInfo.offset,
                                    size = bufferInfo.size,
                                    presentationTimeMs = bufferInfo.presentationTimeUs / 1_000L,
                                    channelCount = outputChannels,
                                    pcmEncoding = pcmEncoding
                                )
                            }
                        }
                        outputEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            return collector.toResult(outputSampleRate)
        } finally {
            try { extractor.release() } catch (_: Exception) { }
            try { decoder?.release() } catch (_: Exception) { }
        }
    }

    private fun String.toInputUri(): Uri =
        if (contains("://")) Uri.parse(this) else Uri.fromFile(File(this))

    private fun MediaFormat.integerOrDefault(key: String, defaultValue: Int): Int =
        if (containsKey(key)) getInteger(key) else defaultValue

    private class EnergyCollector {
        private val windows = mutableListOf<EnergyWindow>()
        private var peak = 0f
        private var sumSquares = 0.0
        private var sampleCount = 0L

        fun consume(
            output: ByteBuffer,
            offset: Int,
            size: Int,
            presentationTimeMs: Long,
            channelCount: Int,
            pcmEncoding: Int
        ) {
            output.order(ByteOrder.LITTLE_ENDIAN)
            output.position(offset)
            output.limit(offset + size)
            var blockSquares = 0.0
            var blockSamples = 0

            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                val floats = output.slice().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                while (floats.hasRemaining()) {
                    val value = floats.get().coerceIn(-1f, 1f)
                    blockSquares += value * value
                    peak = max(peak, abs(value))
                    blockSamples++
                }
            } else {
                val shorts = output.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                while (shorts.hasRemaining()) {
                    val value = shorts.get().toFloat() / Short.MAX_VALUE
                    blockSquares += value * value
                    peak = max(peak, abs(value))
                    blockSamples++
                }
            }

            if (blockSamples > 0) {
                val rms = sqrt(blockSquares / blockSamples).toFloat()
                windows += EnergyWindow(presentationTimeMs, rms)
                sumSquares += blockSquares
                sampleCount += blockSamples.toLong()
            }
        }

        fun toResult(sampleRate: Int): Result {
            if (windows.isEmpty() || sampleCount == 0L) {
                return Result(0f, 0f, 0L, 0L, 0L, 0L, 0f, 0f, emptyList(), emptyList())
            }
            val rms = sqrt(sumSquares / sampleCount).toFloat()
            val lufsApprox = if (rms > 0f) (20f * kotlin.math.log10(rms)) else -70f
            val maxEnergy = windows.maxOf { it.rms }
            val audibleThreshold = max(0.0005f, maxEnergy * 0.06f)
            val cueIn = windows.firstOrNull { it.rms >= audibleThreshold }?.positionMs ?: 0L
            val cueOut = windows.lastOrNull { it.rms >= audibleThreshold }?.positionMs ?: 0L
            val durationMs = windows.last().positionMs
            val introSilence = cueIn
            val outroSilence = (durationMs - cueOut).coerceAtLeast(0L)
            val beats = detectBeats(windows)
            val (bpm, confidence) = estimateTempo(beats)

            return Result(
                bpm = bpm,
                bpmConfidence = confidence,
                cueInMs = cueIn,
                cueOutMs = cueOut,
                introSilenceMs = introSilence,
                outroSilenceMs = outroSilence,
                integratedLufsApprox = lufsApprox,
                truePeak = peak,
                beatPositionsMs = beats,
                energyWindows = windows
            )
        }

        private fun detectBeats(values: List<EnergyWindow>): List<Long> {
            if (values.size < 5) return emptyList()
            val average = values.map { it.rms }.average().toFloat()
            val threshold = max(average * 1.45f, 0.002f)
            val result = mutableListOf<Long>()
            var lastBeatMs = Long.MIN_VALUE
            for (index in 1 until values.lastIndex) {
                val current = values[index]
                val isPeak = current.rms >= threshold && current.rms > values[index - 1].rms && current.rms >= values[index + 1].rms
                if (isPeak && current.positionMs - lastBeatMs >= 250L) {
                    result += current.positionMs
                    lastBeatMs = current.positionMs
                }
            }
            return result
        }

        private fun estimateTempo(beats: List<Long>): Pair<Float, Float> {
            if (beats.size < 4) return 0f to 0f
            val intervals = beats.zipWithNext { first, second -> second - first }
                .filter { it in 250L..1_500L }
                .sorted()
            if (intervals.size < 3) return 0f to 0f
            val median = intervals[intervals.size / 2].toFloat()
            var bpm = 60_000f / median
            while (bpm < MIN_BPM) bpm *= 2f
            while (bpm > MAX_BPM) bpm /= 2f
            val meanDeviation = intervals.map { abs(it - median) / median }.average().toFloat()
            val confidence = min(1f, (intervals.size / 16f) * (1f - meanDeviation).coerceIn(0f, 1f))
            return bpm to confidence
        }
    }
}
