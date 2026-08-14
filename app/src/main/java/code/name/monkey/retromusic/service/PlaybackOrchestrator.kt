package code.name.monkey.retromusic.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import code.name.monkey.retromusic.automix.TidalApiClient
import code.name.monkey.retromusic.db.AutomixAnalysisDao
import code.name.monkey.retromusic.db.TrackAnalysisEntity
import code.name.monkey.retromusic.db.TransitionPlanEntity
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.playback.Playback
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.workers.TrackAnalysisWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.java.KoinJavaComponent
import kotlin.math.cos
import kotlin.math.sin

/**
 * Punto único de reproducción local. En modo normal usa la cola de Media3 para
 * reproducción continua. Con AutoMix activo pre-carga un segundo deck y aplica
 * el TransitionPlan de Room; los motores anteriores permanecen sin borrar para
 * facilitar reversión durante la migración.
 */
class PlaybackOrchestrator(private val context: Context) : Playback {
    enum class SessionReason { NONE, CLUB, INFINITE_RADIO, SMART_DJ }

    data class AutomixState(
        val globalEnabled: Boolean,
        val sessionReason: SessionReason,
        val transitionRunning: Boolean
    ) {
        val active: Boolean get() = globalEnabled || sessionReason != SessionReason.NONE
    }

    /** Estado emitido por el mismo ciclo que modifica los volúmenes del crossfade real. */
    data class AutoMixTransitionState(
        val isRunning: Boolean = false,
        val progress: Float = 0f,
        val outgoing: Song? = null,
        val incoming: Song? = null,
    )

    private data class TransitionSpec(
        val startMs: Long,
        val targetStartMs: Long,
        val durationMs: Long,
        val tempoRatio: Float,
        val confidence: Float,
        val safeFallback: Boolean = false
    )

    override var callbacks: Playback.PlaybackCallbacks? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private var activePlayer = createPlayer()
    private var preloadPlayer = createPlayer()
    private var currentSong: Song? = null
    private var nextSong: Song? = null
    private var nextDataSource: Uri? = null
    private var manualCrossfadeMs = PreferenceUtil.crossFadeDuration.coerceAtLeast(3) * 1_000L
    private var transitionCurveMode = "AUTO_IA"
    private var beatmatchEnabled = true
    private var sessionReason = SessionReason.NONE
    private var transitionRunning = false
    private var pendingPlan: TransitionSpec? = null
    private var pendingPrepare: ((Boolean) -> Unit)? = null
    private var monitor: Runnable? = null
    private var fade: Runnable? = null
    private val _automixTransitionState = MutableStateFlow(AutoMixTransitionState())
    val automixTransitionState: StateFlow<AutoMixTransitionState> = _automixTransitionState.asStateFlow()

    private val analysisDao: AutomixAnalysisDao? by lazy {
        try { KoinJavaComponent.get(AutomixAnalysisDao::class.java) as AutomixAnalysisDao } catch (e: Exception) { null }
    }

    override val isInitialized: Boolean
        get() = activePlayer.playbackState != Player.STATE_IDLE
    override val isPlaying: Boolean
        get() = activePlayer.isPlaying || preloadPlayer.isPlaying
    override val audioSessionId: Int
        get() = activePlayer.audioSessionId
    /** Posición de Media3 expuesta para consumidores visuales como letras sincronizadas. */
    val currentPositionMs: Long
        get() = activePlayer.currentPosition.coerceAtLeast(0L)

    fun state(): AutomixState = AutomixState(PreferenceUtil.isAutomixEnabled, sessionReason, transitionRunning)

    fun setGlobalAutomixEnabled(enabled: Boolean) {
        if (!enabled && sessionReason == SessionReason.CLUB) sessionReason = SessionReason.NONE
        if (enabled) startMonitor() else if (sessionReason == SessionReason.NONE) stopTransition()
    }

    fun toggleClubMode(): Boolean {
        sessionReason = if (state().active) SessionReason.NONE else SessionReason.CLUB
        if (state().active) startMonitor() else stopTransition()
        return state().active
    }

    fun activateForSession(reason: SessionReason) {
        sessionReason = reason
        startMonitor()
    }

    fun clearSessionOverride() {
        sessionReason = SessionReason.NONE
        if (!PreferenceUtil.isAutomixEnabled) stopTransition()
    }

    fun isCurrentSongLoaded(song: Song): Boolean = currentSong?.id == song.id && isInitialized

    fun setNextSong(song: Song?) {
        nextSong = song
        nextDataSource = song?.uri
        pendingPlan = null
        if (song == null) return
        if (!state().active) {
            activePlayer.removeMediaItems(1, activePlayer.mediaItemCount)
            activePlayer.addMediaSource(createMediaSource(song))
        } else {
            resolvePlanAsync()
            startMonitor()
        }
    }

    override fun setDataSource(song: Song, force: Boolean, completion: (success: Boolean) -> Unit) {
        if (!force && isCurrentSongLoaded(song)) {
            completion(true)
            return
        }
        handler.post {
            stopTransition()
            currentSong = song
            pendingPrepare = completion
            activePlayer.stop()
            activePlayer.clearMediaItems()
            activePlayer.setMediaSource(createMediaSource(song))
            activePlayer.prepare()
            if (state().active) resolvePlanAsync()
        }
    }

    override fun setNextDataSource(path: Uri?) {
        nextDataSource = path
    }

    override fun start(): Boolean {
        activePlayer.play()
        if (state().active) startMonitor()
        callbacks?.onPlayStateChanged()
        return true
    }

    override fun pause(): Boolean {
        activePlayer.pause()
        preloadPlayer.pause()
        callbacks?.onPlayStateChanged()
        return true
    }

    override fun stop() {
        stopTransition()
        activePlayer.stop()
        preloadPlayer.stop()
    }

    override fun release() {
        stopTransition()
        scope.cancel()
        activePlayer.release()
        preloadPlayer.release()
    }

    override fun duration(): Int = activePlayer.duration.takeIf { it != C.TIME_UNSET }?.toInt() ?: -1
    override fun position(): Int = activePlayer.currentPosition.toInt()

    override fun seek(whereto: Int, force: Boolean): Int {
        if (force) stopTransition()
        activePlayer.seekTo(whereto.toLong())
        return whereto
    }

    override fun setVolume(vol: Float): Boolean {
        activePlayer.volume = vol.coerceIn(0f, 1f)
        return true
    }

    override fun setAudioSessionId(sessionId: Int): Boolean = false

    override fun setCrossFadeDuration(duration: Int) {
        manualCrossfadeMs = duration.coerceIn(3, 12) * 1_000L
    }

    fun setTransitionSettings(durationSeconds: Int, curveMode: String, enableBeatmatch: Boolean) {
        manualCrossfadeMs = durationSeconds.coerceIn(3, 12) * 1_000L
        transitionCurveMode = curveMode
        beatmatchEnabled = enableBeatmatch
    }

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        if (!transitionRunning) activePlayer.playbackParameters = PlaybackParameters(speed, pitch)
    }

    private fun createPlayer(): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        pendingPrepare?.invoke(true)
                        pendingPrepare = null
                    }
                    if (playbackState == Player.STATE_ENDED && !transitionRunning) callbacks?.onTrackEnded()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val next = nextSong ?: return
                    if (!state().active && mediaItem?.mediaId == next.id.toString()) {
                        currentSong = next
                        nextSong = null
                        callbacks?.onTrackWentToNext()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    pendingPrepare?.invoke(false)
                    pendingPrepare = null
                }
            })
        }

    private fun createMediaSource(song: Song): MediaSource {
        val path = song.data.trim()
        val item = MediaItem.Builder().setMediaId(song.id.toString()).setUri(song.uri).build()
        if (!path.startsWith("tidal://track/", true)) {
            return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)).createMediaSource(item)
        }
        val trackId = path.removePrefix("tidal://track/").substringBefore("::")
        val upstream = DefaultDataSource.Factory(context)
        val resolver = ResolvingDataSource.Resolver { spec ->
            val streamUrl = runBlocking(Dispatchers.IO) { TidalApiClient.getStreamUrl(trackId) }
            streamUrl?.let { spec.withUri(Uri.parse(it)) } ?: spec
        }
        return ProgressiveMediaSource.Factory(ResolvingDataSource.Factory(upstream, resolver)).createMediaSource(item)
    }

    private fun resolvePlanAsync() {
        val from = currentSong ?: return
        val to = nextSong ?: return
        scope.launch {
            val spec = withContext(Dispatchers.IO) {
                val fromAnalysis = analysisDao?.findAnalysisBySourceUri(TrackAnalysisWorker.normalizedSourceUri(from.data))
                val toAnalysis = analysisDao?.findAnalysisBySourceUri(TrackAnalysisWorker.normalizedSourceUri(to.data))
                val plan = if (fromAnalysis != null && toAnalysis != null) {
                    analysisDao?.getLatestTransitionPlan(fromAnalysis.analysisId, toAnalysis.analysisId)
                } else null
                prepareNextTransition(fromAnalysis, toAnalysis, plan)
            }
            pendingPlan = spec
        }
    }

    /**
     * Nunca habilita beatmatching si la pista entrante carece de BPM fiable. Registra un
     * plan de contingencia en Room cuando existen ambas entidades para que el estado sea auditable.
     */
    private suspend fun prepareNextTransition(
        from: TrackAnalysisEntity?,
        nextTrack: TrackAnalysisEntity?,
        storedPlan: TransitionPlanEntity?
    ): TransitionSpec {
        if (!hasTrustedTempo(from) || !hasTrustedTempo(nextTrack)) {
            if (storedPlan?.strategy == STRATEGY_SAFE_FALLBACK) {
                return storedPlan.toSpec(from, nextTrack)
            }
            val fallback = TransitionPlanEntity(
                fromAnalysisId = from?.analysisId ?: 0L,
                toAnalysisId = nextTrack?.analysisId ?: 0L,
                strategy = STRATEGY_SAFE_FALLBACK,
                transitionStartMs = 0L,
                targetStartMs = nextTrack?.cueInMs.orZero(),
                beatCount = 0,
                tempoRatio = 1f,
                confidence = 0f,
                explanation = "BPM ausente, cero o sin confianza suficiente: crossfade lineal seguro."
            )
            if (fallback.fromAnalysisId > 0L && fallback.toAnalysisId > 0L) {
                analysisDao?.upsertTransitionPlan(fallback)
            }
            return fallback.toSpec(from, nextTrack)
        }
        return storedPlan.toSpec(from, nextTrack)
    }

    private fun hasTrustedTempo(analysis: TrackAnalysisEntity?): Boolean =
        analysis != null && analysis.bpm > 0f && analysis.bpmConfidence >= MIN_TRUSTED_BPM_CONFIDENCE

    private fun TransitionPlanEntity?.toSpec(
        from: TrackAnalysisEntity?,
        to: TrackAnalysisEntity?
    ): TransitionSpec {
        if (this?.strategy == STRATEGY_SAFE_FALLBACK) {
            return TransitionSpec(
                startMs = 0L,
                targetStartMs = to?.cueInMs.orZero(),
                durationMs = SAFE_FALLBACK_CROSSFADE_MS,
                tempoRatio = 1f,
                confidence = 0f,
                safeFallback = true
            )
        }
        val ratio = this?.tempoRatio ?: run {
            if ((from?.bpm ?: 0f) > 0f && (to?.bpm ?: 0f) > 0f) {
                (from!!.bpm / to!!.bpm).coerceIn(0.92f, 1.08f)
            } else {
                1f
            }
        }
        return TransitionSpec(
            startMs = this?.transitionStartMs ?: 0L,
            targetStartMs = this?.targetStartMs ?: to?.cueInMs.orZero(),
            durationMs = (this?.beatCount?.times(500L) ?: manualCrossfadeMs).coerceIn(3_000L, 12_000L),
            tempoRatio = ratio,
            confidence = this?.confidence ?: from?.bpmConfidence.orZero(),
            safeFallback = false
        )
    }

    private fun startMonitor() {
        if (!state().active || monitor != null) return
        monitor = object : Runnable {
            override fun run() {
                maybeStartTransition()
                if (state().active && (activePlayer.isPlaying || transitionRunning)) handler.postDelayed(this, 100L) else monitor = null
            }
        }
        handler.post(monitor!!)
    }

    private fun maybeStartTransition() {
        if (!state().active || transitionRunning || nextSong == null) return
        val duration = activePlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return
        val spec = pendingPlan ?: TransitionSpec(
            startMs = (duration - manualCrossfadeMs).coerceAtLeast(0L),
            targetStartMs = 0L,
            durationMs = SAFE_FALLBACK_CROSSFADE_MS,
            tempoRatio = 1f,
            confidence = 0f,
            safeFallback = true
        )
        val trigger = if (spec.startMs in 1 until duration) spec.startMs else (duration - spec.durationMs).coerceAtLeast(0L)
        if (activePlayer.currentPosition >= trigger) startTransition(spec)
    }

    private fun startTransition(spec: TransitionSpec) {
        val incoming = nextSong ?: return
        transitionRunning = true
        _automixTransitionState.value = AutoMixTransitionState(true, 0f, currentSong, incoming)
        preloadPlayer.stop()
        preloadPlayer.clearMediaItems()
        preloadPlayer.volume = 0f
        preloadPlayer.setMediaSource(createMediaSource(incoming))
        val initialTempoRatio = if (beatmatchEnabled && !spec.safeFallback) spec.tempoRatio else 1f
        preloadPlayer.playbackParameters = PlaybackParameters(initialTempoRatio, 1f)
        preloadPlayer.prepare()
        preloadPlayer.playWhenReady = true
        if (spec.targetStartMs > 0L) preloadPlayer.seekTo(spec.targetStartMs)
        val startedAt = System.currentTimeMillis()
        fade = object : Runnable {
            override fun run() {
                val progress = ((System.currentTimeMillis() - startedAt).toFloat() / spec.durationMs).coerceIn(0f, 1f)
                val (outFactor, inFactor) = volumeFactors(progress, spec.safeFallback)
                activePlayer.volume = outFactor
                preloadPlayer.volume = inFactor
                _automixTransitionState.value = AutoMixTransitionState(true, progress, currentSong, incoming)
                val currentTempo = initialTempoRatio + (1f - initialTempoRatio) * progress
                preloadPlayer.playbackParameters = PlaybackParameters(currentTempo, 1f)
                if (progress >= 1f) completeTransition(incoming) else handler.postDelayed(this, 50L)
            }
        }
        handler.post(fade!!)
    }

    private fun completeTransition(incoming: Song) {
        activePlayer.stop()
        activePlayer.clearMediaItems()
        val old = activePlayer
        activePlayer = preloadPlayer
        preloadPlayer = old
        activePlayer.volume = 1f
        activePlayer.playbackParameters = PlaybackParameters(1f, 1f)
        currentSong = incoming
        nextSong = null
        pendingPlan = null
        transitionRunning = false
        fade = null
        _automixTransitionState.value = AutoMixTransitionState()
        callbacks?.onTrackEndedWithCrossfade()
    }

    private fun stopTransition() {
        monitor?.let(handler::removeCallbacks)
        fade?.let(handler::removeCallbacks)
        monitor = null
        fade = null
        transitionRunning = false
        _automixTransitionState.value = AutoMixTransitionState()
        handler.post {
            preloadPlayer.pause()
            preloadPlayer.volume = 1f
        }
    }

    private fun volumeFactors(progress: Float, safeFallback: Boolean): Pair<Float, Float> {
        if (safeFallback) return (1f - progress) to progress
        return when (transitionCurveMode) {
        "HIGH_ENERGY" -> (1f - progress * progress).coerceIn(0f, 1f) to kotlin.math.sqrt(progress)
        "HARMONIC" -> (1f - progress) to progress
        "EQUAL_POWER", "AUTO_IA" -> {
            cos(progress * Math.PI.toFloat() / 2f) to sin(progress * Math.PI.toFloat() / 2f)
        }
        else -> (1f - progress) to progress
        }
    }

    private fun Long?.orZero(): Long = this ?: 0L
    private fun Float?.orZero(): Float = this ?: 0f

    private companion object {
        const val MIN_TRUSTED_BPM_CONFIDENCE = 0.80f
        const val SAFE_FALLBACK_CROSSFADE_MS = 3_500L
        const val STRATEGY_SAFE_FALLBACK = "SAFE_FALLBACK_LINEAR"
    }
}
