/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.automix

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import code.name.monkey.retromusic.db.SongEntity

import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Motor de Audio Doble (Dual Audio Engine) para reproducción Automix estilo DJ.
 *
 * Administra dos instancias en paralelo de [ExoPlayer] (`primaryPlayer` y `secondaryPlayer`)
 * y realiza transiciones (crossfade automático + beatmatching armónico sin distorsión de voz)
 * cuando la pista actual alcanza `durationMs - cueOutMs`.
 */
class AutomixPlayerEngine(private val context: Context) {

    companion object {
        private const val TAG = "AutomixPlayerEngine"
        private const val MONITOR_INTERVAL_MS = 100L
        private const val CROSSFADE_STEP_MS = 50L
        private const val MIN_REMAINING_FOR_TRANSITION_MS = 1500L

        @Volatile
        private var INSTANCE: AutomixPlayerEngine? = null

        fun getInstance(context: Context): AutomixPlayerEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutomixPlayerEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    interface AutomixListener {
        fun onSongTransitionStarted(from: SongEntity, to: SongEntity)
        fun onSongTransitionCompleted(current: SongEntity)
        fun onPlaybackError(error: Exception)
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    /**
     * Reproductor principal en emisión.
     */
    var primaryPlayer: ExoPlayer = createExoPlayer()
        private set

    /**
     * Reproductor secundario en cola para preparación y transición en segundo plano.
     */
    var secondaryPlayer: ExoPlayer = createExoPlayer()
        private set

    var listener: AutomixListener? = null

    var manualCrossfadeDurationMs: Long = 6000L
    var transitionCurveMode: String = "AUTO_IA"
    var isBeatmatchEnabled: Boolean = true

    private var currentSong: SongEntity? = null
    private var nextSong: SongEntity? = null

    private var isTransitioning = false
    private val handler = Handler(Looper.getMainLooper())

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkAndTriggerAutomix()
            if (primaryPlayer.isPlaying || isTransitioning) {
                handler.postDelayed(this, MONITOR_INTERVAL_MS)
            }
        }
    }

    private var crossfadeRunnable: Runnable? = null

    private fun createExoPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setWakeMode(C.WAKE_MODE_LOCAL)
            }
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS ||
                        reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)) {
                    pause()
                }
            }
        })
        return player
    }

    /**
     * Prepara el [ExoPlayer] suministrado para reproducir una pista local o stream Deezer al vuelo.
     */
    private fun preparePlayerForSong(player: ExoPlayer, song: SongEntity): Boolean {
        val path = song.data.trim()
        player.stop()
        player.clearMediaItems()
        return if (path.startsWith("deezer://track/", true)) {
            val trackId = path.substringAfter("deezer://track/")
            val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
            
            // Create a ResolvingDataSource to fetch the real stream URL
            val resolver = androidx.media3.datasource.ResolvingDataSource.Resolver { dataSpec ->
                val streamUrl = kotlinx.coroutines.runBlocking {
                    // Fetch private track data to get trackToken, then get stream url
                    val track = code.name.monkey.retromusic.automix.DeezerApiClient.fetchPrivateTrackData(trackId)
                    if (track != null) {
                        val prefQuality = code.name.monkey.retromusic.fragments.settings.MillaySettingsFragment.getStreamingQuality(context).uppercase()
                        var url = code.name.monkey.retromusic.automix.DeezerApiClient.getStreamUrl(track, prefQuality)
                        if (url == null && prefQuality != "MP3_320") {
                            Log.d(TAG, "Fallback: stream con calidad $prefQuality falló. Intentando MP3_320...")
                            url = code.name.monkey.retromusic.automix.DeezerApiClient.getStreamUrl(track, "MP3_320")
                        }
                        if (url == null && prefQuality != "MP3_128") {
                            Log.d(TAG, "Fallback: stream con MP3_320 falló. Intentando MP3_128...")
                            url = code.name.monkey.retromusic.automix.DeezerApiClient.getStreamUrl(track, "MP3_128")
                        }
                        url
                    } else null
                }
                if (streamUrl != null) dataSpec.withUri(Uri.parse(streamUrl)) else dataSpec
            }
            
            val resolvingFactory = androidx.media3.datasource.ResolvingDataSource.Factory(upstreamFactory, resolver)
            
            // Wrap it in DeezerDataSourceFactory to decrypt the Blowfish stream on the fly
            val deezerFactory = createDeezerDataSourceFactory(resolvingFactory, trackId)
            val mediaItem = MediaItem.fromUri(Uri.parse(path))
            val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(deezerFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            true
        } else if (path.startsWith("tidal://track/", true)) {
            val split = path.removePrefix("tidal://track/").split("::")
            val trackId = split[0]
            val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
            val resolver = androidx.media3.datasource.ResolvingDataSource.Resolver { dataSpec ->
                val streamUrl = kotlinx.coroutines.runBlocking {
                    TidalApiClient.getStreamUrl(trackId)
                }
                if (streamUrl != null) dataSpec.withUri(Uri.parse(streamUrl)) else dataSpec
            }
            val tidalFactory = androidx.media3.datasource.ResolvingDataSource.Factory(upstreamFactory, resolver)
            val mediaItem = MediaItem.fromUri(Uri.parse(path))
            val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(tidalFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            true
        } else {
            val mediaItem = buildMediaItem(song)
            if (mediaItem != null) {
                player.setMediaItem(mediaItem)
                true
            } else {
                false
            }
        }
    }

    /**
     * Crea un [MediaItem] compatible con archivos locales y streams HTTP/HTTPS/Deezer.
     */
    private fun buildMediaItem(song: SongEntity): MediaItem? {
        val path = song.data.trim()
        return if (path.startsWith("http://", true) || path.startsWith("https://", true) || path.startsWith("deezer://", true) || path.startsWith("tidal://", true)) {
            MediaItem.fromUri(Uri.parse(path))
        } else {
            val file = File(path)
            if (!file.exists()) return null
            MediaItem.fromUri(Uri.fromFile(file))
        }
    }

    /**
     * Crea un [DataSource.Factory] envolviendo cualquier factory upstream con [DeezerDataSource]
     * para descifrar paquetes Blowfish de Deezer en memoria al vuelo sin servidores intermedios.
     */
    fun createDeezerDataSourceFactory(
        upstreamFactory: DataSource.Factory,
        trackId: String
    ): DataSource.Factory = DataSource.Factory {
        DeezerDataSource(upstreamFactory.createDataSource(), trackId)
    }


    /**
     * Carga y reproduce de inmediato la pista principal, asignando opcionalmente la siguiente.
     */
    fun loadAndPlay(song: SongEntity, next: SongEntity? = null) {
        stopTransition()
        currentSong = song
        nextSong = next
        isTransitioning = false

        primaryPlayer.volume = getGainMultiplier(song.replayGain)
        primaryPlayer.playbackParameters = PlaybackParameters(1.0f, 1.0f)

        if (!preparePlayerForSong(primaryPlayer, song)) {
            listener?.onPlaybackError(IllegalArgumentException("Archivo o stream no accesible: ${song.data}"))
            return
        }

        primaryPlayer.prepare()
        primaryPlayer.play()



        startMonitoring()
    }

    /**
     * Define o actualiza la siguiente pista que será mezclada en Automix.
     */
    fun setNextSong(next: SongEntity?) {
        this.nextSong = next
    }

    /**
     * Pausa ambos reproductores.
     */
    fun pause() {
        primaryPlayer.pause()
        if (isTransitioning) {
            secondaryPlayer.pause()
        }
        stopMonitoring()
    }

    /**
     * Reanuda la reproducción.
     */
    fun resume() {
        primaryPlayer.play()
        if (isTransitioning) {
            secondaryPlayer.play()
        }
        startMonitoring()
    }

    /**
     * Detiene la reproducción de ambos reproductores.
     */
    fun stop() {
        stopTransition()
        primaryPlayer.stop()
        secondaryPlayer.stop()
        stopMonitoring()
    }

    /**
     * Libera los recursos de ambos reproductores [ExoPlayer].
     */
    fun release() {
        stopTransition()
        stopMonitoring()
        primaryPlayer.release()
        secondaryPlayer.release()
    }

    fun isPlaying(): Boolean = primaryPlayer.isPlaying || secondaryPlayer.isPlaying

    fun getCurrentPosition(): Long = primaryPlayer.currentPosition

    fun getDuration(): Long = primaryPlayer.duration

    fun getCurrentSong(): SongEntity? = currentSong

    fun getNextSong(): SongEntity? = nextSong

    // -- Lógica Interna de Monitoreo y Crossfade DJ --

    private fun startMonitoring() {
        handler.removeCallbacks(monitorRunnable)
        handler.postDelayed(monitorRunnable, MONITOR_INTERVAL_MS)
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(monitorRunnable)
    }

    private fun checkAndTriggerAutomix() {
        if (!code.name.monkey.retromusic.util.PreferenceUtil.isAutomixEnabled) return
        if (isTransitioning) return
        val current = currentSong ?: return
        val next = nextSong ?: return

        val durationMs = primaryPlayer.duration
        val currentPositionMs = primaryPlayer.currentPosition
        if (durationMs <= 0L || currentPositionMs <= 0L) return

        val cueOutMs = if (current.cueOutMs > 0L) current.cueOutMs else 5000L
        val triggerPositionMs = durationMs - cueOutMs
        val remainingMs = durationMs - currentPositionMs

        if (currentPositionMs >= triggerPositionMs && remainingMs > MIN_REMAINING_FOR_TRANSITION_MS) {
            startAutomixTransition(current, next, cueOutMs)
        }
    }

    /**
     * Inicia la preparación del reproductor secundario y el crossfade dinámico.
     */
    private fun startAutomixTransition(from: SongEntity, to: SongEntity, transitionDurationMs: Long) {
        isTransitioning = true
        listener?.onSongTransitionStarted(from, to)

        secondaryPlayer.volume = 0f
        if (!preparePlayerForSong(secondaryPlayer, to)) {
            Log.w(TAG, "No se puede iniciar Automix: archivo o stream siguiente no accesible ${to.data}")
            return
        }



        // Beatmatch armónico sin distorsionar voz: ajustar speed al ratio de BPM si ambos son válidos (> 0f)
        val bpmA = from.bpm
        val bpmB = to.bpm
        val initialSpeed = if (bpmA > 0f && bpmB > 0f) {
            (bpmA / bpmB).coerceIn(0.85f, 1.15f)
        } else {
            1.0f
        }
        // pitch = 1.0f preserva la tonalidad natural vocal gracias al time-stretching de Media3
        secondaryPlayer.playbackParameters = PlaybackParameters(initialSpeed, 1.0f)

        secondaryPlayer.prepare()
        secondaryPlayer.play()

        val primaryGainMult = getGainMultiplier(from.replayGain)
        val secondaryGainMult = getGainMultiplier(to.replayGain)
        val genreCurve = selectGenreCurve(to)
        val startTimeMs = System.currentTimeMillis()
        val totalDurationMs = transitionDurationMs.coerceIn(3000L, 12000L)

        crossfadeRunnable = object : Runnable {
            override fun run() {
                val elapsedMs = System.currentTimeMillis() - startTimeMs
                val progress = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

                // 1. Interpolación de volumen por curva de género / Equal Power
                applyCrossfadeVolumes(progress, primaryGainMult, secondaryGainMult, genreCurve)

                // 2. Convergencia suave del speed hacia 1.0f (tempo natural de la pista B)
                if (initialSpeed != 1.0f) {
                    val currentSpeed = initialSpeed + (1.0f - initialSpeed) * progress
                    secondaryPlayer.playbackParameters = PlaybackParameters(currentSpeed, 1.0f)
                }

                if (progress >= 1.0f) {
                    completeTransitionAndSwap()
                } else {
                    handler.postDelayed(this, CROSSFADE_STEP_MS)
                }
            }
        }
        handler.post(crossfadeRunnable!!)
    }

    private fun applyCrossfadeVolumes(
        progress: Float,
        primaryGain: Float,
        secondaryGain: Float,
        curveType: GenreCurve
    ) {
        val (primaryFactor, secondaryFactor) = when (curveType) {
            // Alta energía (Latino, Salsa, Merengue, Bomba): Entrada enérgica rápida y caída parabólica
            GenreCurve.HIGH_ENERGY -> {
                val inFactor = sqrt(progress)
                val outFactor = (1f - progress * progress)
                Pair(outFactor, inFactor)
            }
            // Armónica (Pop, Reguetón, Electrónica): Crossfade lineal balanceado
            GenreCurve.HARMONIC_LINEAR -> {
                Pair(1f - progress, progress)
            }
            // Equal Power (Rock, Baladas, Default BPM=0): Mantiene energía RMS constante sin caídas
            GenreCurve.EQUAL_POWER -> {
                val outFactor = cos(progress * (Math.PI / 2)).toFloat()
                val inFactor = sin(progress * (Math.PI / 2)).toFloat()
                Pair(outFactor, inFactor)
            }
        }

        primaryPlayer.volume = (primaryFactor * primaryGain).coerceIn(0f, 1f)
        secondaryPlayer.volume = (secondaryFactor * secondaryGain).coerceIn(0f, 1f)
    }

    /**
     * Intercambia los roles de ambos reproductores: el secundario pasa a ser el primario
     * y el primario anterior se detiene y libera de su pista.
     */
    private fun completeTransitionAndSwap() {
        stopTransition()

        // 1. Detener y limpiar el antiguo primario
        primaryPlayer.stop()
        primaryPlayer.clearMediaItems()
        primaryPlayer.volume = 1.0f

        // 2. Swap de referencias entre reproductores
        val temp = primaryPlayer
        primaryPlayer = secondaryPlayer
        secondaryPlayer = temp

        // 3. Confirmar parámetros naturales en el nuevo primario
        val newCurrent = nextSong
        currentSong = newCurrent
        nextSong = null

        val finalGain = getGainMultiplier(newCurrent?.replayGain ?: 0f)
        primaryPlayer.volume = finalGain
        primaryPlayer.playbackParameters = PlaybackParameters(1.0f, 1.0f)

        isTransitioning = false

        if (newCurrent != null) {
            listener?.onSongTransitionCompleted(newCurrent)
        }
    }

    private fun stopTransition() {
        crossfadeRunnable?.let { handler.removeCallbacks(it) }
        crossfadeRunnable = null
        isTransitioning = false
    }

    private fun getGainMultiplier(replayGainDb: Float): Float {
        if (replayGainDb == 0f || replayGainDb.isNaN()) return 1.0f
        return 10f.pow(replayGainDb / 20f).coerceIn(0.25f, 1.5f)
    }

    private enum class GenreCurve {
        HIGH_ENERGY,
        HARMONIC_LINEAR,
        EQUAL_POWER
    }

    private fun selectGenreCurve(song: SongEntity): GenreCurve {
        if (song.bpm <= 0f) return GenreCurve.EQUAL_POWER
        val lowerGenre = song.albumName.lowercase(Locale.ROOT)
        return when {
            lowerGenre.contains("salsa") || lowerGenre.contains("merengue") ||
                lowerGenre.contains("bomba") || lowerGenre.contains("bachata") ||
                lowerGenre.contains("cumbia") || lowerGenre.contains("tropical") -> GenreCurve.HIGH_ENERGY
            lowerGenre.contains("pop") || lowerGenre.contains("reguet") ||
                lowerGenre.contains("reggaet") || lowerGenre.contains("electr") ||
                lowerGenre.contains("dance") || lowerGenre.contains("edm") ||
                lowerGenre.contains("house") || lowerGenre.contains("techno") ||
                lowerGenre.contains("k-pop") -> GenreCurve.HARMONIC_LINEAR
            else -> GenreCurve.EQUAL_POWER
        }
    }
}
