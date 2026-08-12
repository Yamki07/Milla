package code.name.monkey.retromusic.service

import android.animation.Animator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.PowerManager
import androidx.core.net.toUri
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.repository.RoomRepository
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.AudioFader.Companion.createFadeAnimator
import code.name.monkey.retromusic.service.playback.Playback.PlaybackCallbacks
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.PreferenceUtil.playbackPitch
import code.name.monkey.retromusic.util.PreferenceUtil.playbackSpeed
import code.name.monkey.retromusic.util.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** @author Prathamesh M */

/*
* To make Crossfade work we need two MediaPlayer's
* Basically, we switch back and forth between those two mp's
* e.g. When song is about to end (Reaches Crossfade duration) we let current mediaplayer
* play but with decreasing volume and start the player with the next song with increasing volume
* and vice versa for upcoming song and so on.
*/
class CrossFadePlayer(context: Context) : AudioManagerPlayback(context),
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private var currentPlayer: CurrentPlayer = CurrentPlayer.NOT_SET
    private var player1 = MediaPlayer()
    private var player2 = MediaPlayer()
    private var durationListener = DurationListener()
    private var mIsInitialized = false
    private var hasDataSource: Boolean = false /* Whether first player has DataSource */
    private var nextDataSource: String? = null
    private var crossFadeAnimator: Animator? = null
    override var callbacks: PlaybackCallbacks? = null
    private var crossFadeDuration = PreferenceUtil.crossFadeDuration
    var isCrossFading = false

    /** Perfil avanzado (Nivel 2+) para la canción A (actual). */
    @Volatile
    private var currentAutomixProfile: code.name.monkey.retromusic.automix.AdvancedAutomixProfile? = null

    /** Perfil avanzado (Nivel 2+) para la canción B (siguiente). */
    @Volatile
    private var nextAutomixProfile: code.name.monkey.retromusic.automix.AdvancedAutomixProfile? = null

    /** Posición absoluta (ms) donde debe iniciar el fade-out Automix. 0 = no definido. */
    @Volatile
    private var automixCueOutMs: Long = 0L

    /** Silencio de outro detectado (ms). Usado como fallback si cueOutMs == 0. */
    @Volatile
    private var automixOutroSilenceDurationMs: Long = 0L

    /** Evita disparar la transición Automix más de una vez por pista. */
    @Volatile
    private var automixTransitionTriggered: Boolean = false

    private val automixScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var automixLoadJob: Job? = null

    init {
        player1.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player2.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        currentPlayer = CurrentPlayer.PLAYER_ONE
    }

    override fun start(): Boolean {
        super.start()
        durationListener.start()
        resumeFade()
        return try {
            getCurrentPlayer()?.start()
            if (isCrossFading) {
                getNextPlayer()?.start()
            }
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override fun release() {
        stop()
        cancelFade()
        resetAutomixState()
        automixLoadJob?.cancel()
        automixScope.cancel()
        getCurrentPlayer()?.release()
        getNextPlayer()?.release()
        durationListener.cancel()
    }

    override fun stop() {
        super.stop()
        getCurrentPlayer()?.reset()
        mIsInitialized = false
        resetAutomixState()
    }

    override fun pause(): Boolean {
        super.pause()
        durationListener.stop()
        pauseFade()
        getCurrentPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        getNextPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        return true
    }

    override fun seek(whereto: Int, force: Boolean): Int {
        if (force) {
            endFade()
        }
        getNextPlayer()?.stop()
        return try {
            getCurrentPlayer()?.seekTo(whereto)
            // Rearmar Automix si el usuario vuelve a un punto anterior al cue-out
            val cueOut = automixCueOutMs
            if (cueOut > 0L && whereto.toLong() < cueOut) {
                automixTransitionTriggered = false
            }
            whereto
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    override fun setVolume(vol: Float): Boolean {
        cancelFade()
        return try {
            getCurrentPlayer()?.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override val isInitialized: Boolean
        get() = mIsInitialized

    override val isPlaying: Boolean
        get() = mIsInitialized && getCurrentPlayer()?.isPlaying == true

    override fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        if (force) hasDataSource = false
        mIsInitialized = false
        resetAutomixState()
        /* We've already set DataSource if initialized is true in setNextDataSource */
        if (!hasDataSource) {
            getCurrentPlayer()?.let {
                setDataSourceImpl(it, song.uri.toString()) { success ->
                    mIsInitialized = success
                    if (success && PreferenceUtil.isAutomixEnabled) {
                        updateAutomixCueOut(song.id)
                    }
                    completion(success)
                }
            }
            hasDataSource = true
        } else {
            if (PreferenceUtil.isAutomixEnabled) {
                updateAutomixCueOut(song.id)
            }
            completion(true)
            mIsInitialized = true
        }
    }

    override fun setNextDataSource(path: Uri?) {
        // Store the next song path in nextDataSource, we'll need this just in case
        // if the user closes the app, then we can't get the nextSong from musicService
        // As MusicPlayerRemote won't have access to the musicService
        nextDataSource = path.toString()
    }

    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            getCurrentPlayer()?.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override val audioSessionId: Int
        get() = getCurrentPlayer()?.audioSessionId!!

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.duration!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * Gets the current position in audio.
     * @return The position in milliseconds
     */
    override fun position(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.currentPosition!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mp == getCurrentPlayer()) {
            callbacks?.onTrackEnded()
        }
    }

    private fun getCurrentPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player1
            }

            CurrentPlayer.PLAYER_TWO -> {
                player2
            }

            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun getNextPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player2
            }

            CurrentPlayer.PLAYER_TWO -> {
                player1
            }

            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun crossFade(fadeInMp: MediaPlayer, fadeOutMp: MediaPlayer) {
        isCrossFading = true
        // AUTO MIX DJ (Nivel 4 - Energy Curve Matching): Si tenemos ambos perfiles, usar curvas
        val cProf = currentAutomixProfile
        val nProf = nextAutomixProfile
        
        // Determinar duración de transición basada en Nivel 2 (Estructuras)
        var dynamicFadeDurationMs = crossFadeDuration * 1000L
        if (cProf != null && nProf != null) {
            // Nivel 2: Si el outro de A es instrumental pero B entra directo con vocal, hacer un fade más rápido.
            if (cProf.endingType == "fade" || cProf.endingType == "instrumental") {
                if (nProf.introStyle == "vocal") {
                    dynamicFadeDurationMs = 2500L // Transición rápida para no ahogar la voz
                } else {
                    dynamicFadeDurationMs = 6000L // Transición suave instrumental a instrumental
                }
            } else if (cProf.endingType == "hard-stop") {
                dynamicFadeDurationMs = 1500L // Transición muy rápida para cortes de golpe
            }
        }

        crossFadeAnimator = createFadeAnimator(context, fadeInMp, fadeOutMp, duration = dynamicFadeDurationMs) {
            crossFadeAnimator = null
            durationListener.start()
            isCrossFading = false
        }
        crossFadeAnimator?.start()
    }

    private fun endFade() {
        crossFadeAnimator?.end()
        crossFadeAnimator = null
    }

    private fun cancelFade() {
        crossFadeAnimator?.cancel()
        crossFadeAnimator = null
    }

    private fun pauseFade() {
        crossFadeAnimator?.pause()
    }

    private fun resumeFade() {
        if (crossFadeAnimator?.isPaused == true) {
            crossFadeAnimator?.resume()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mIsInitialized = false
        mp?.release()
        player1 = MediaPlayer()
        player2 = MediaPlayer()
        mIsInitialized = true
        mp?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        context.showToast(R.string.unplayable_file)
        logE(what.toString() + extra)
        return false
    }

    enum class CurrentPlayer {
        PLAYER_ONE,
        PLAYER_TWO,
        NOT_SET
    }

    inner class DurationListener : CoroutineScope by crossFadeScope() {

        private var job: Job? = null

        fun start() {
            job?.cancel()
            job = launch {
                while (isActive) {
                    delay(250)
                    onDurationUpdated(position(), duration())
                }
            }
        }

        fun stop() {
            job?.cancel()
        }
    }

    fun onDurationUpdated(progress: Int, total: Int) {
        if (progress < 0 || total <= 0) return

        // AUTO MIX DJ: dispara exactamente en cueOutMs (posición absoluta en ms)
        if (PreferenceUtil.isAutomixEnabled && !isCrossFading && !automixTransitionTriggered) {
            val triggerMs = resolveAutomixTriggerMs(total)
            if (triggerMs > 0L && progress.toLong() >= triggerMs) {
                automixTransitionTriggered = true
                triggerAutomixTransition()
                return
            }
        }

        // MODO CROSSFADE CLÁSICO: fallback si Auto Mix está desactivado
        if (!PreferenceUtil.isAutomixEnabled &&
            total > 0 &&
            (total - progress).div(1000) == crossFadeDuration
        ) {
            getNextPlayer()?.let { player ->
                val nextSong = MusicPlayerRemote.nextSong
                if (nextSong != null && nextSong != Song.emptySong) {
                    nextDataSource = null
                    setDataSourceImpl(player, nextSong.uri.toString()) { success ->
                        if (success) switchPlayer()
                    }
                } else if (!nextDataSource.isNullOrEmpty()) {
                    setDataSourceImpl(player, nextDataSource!!) { success ->
                        if (success) switchPlayer()
                        nextDataSource = null
                    }
                }
            }
        }
    }

    /**
     * Resuelve el milisegundo absoluto de disparo Automix.
     * Nivel 3: Usa mixOutPoints avanzados si están disponibles.
     * Preferencia: mixOutPoints > cueOutMs > duration - outro_silence.
     */
    private fun resolveAutomixTriggerMs(totalDurationMs: Int): Long {
        currentAutomixProfile?.mixOutPoints?.firstOrNull { it > 0f }?.let { mixOutSec ->
            val mixOutMs = (mixOutSec * 1000).toLong()
            if (mixOutMs < totalDurationMs) return mixOutMs
        }

        val cueOut = automixCueOutMs
        if (cueOut > 0L) {
            // Clamp al rango reproducible por si Room tiene un valor inconsistente
            return cueOut.coerceIn(1L, totalDurationMs.toLong().coerceAtLeast(1L))
        }
        val outroSilence = automixOutroSilenceDurationMs
        if (outroSilence > 0L && totalDurationMs > outroSilence) {
            return (totalDurationMs.toLong() - outroSilence).coerceAtLeast(1L)
        }
        return 0L
    }

    /**
     * Lanza la transición DJ en el milisegundo de cue-out de la pista actual.
     * Nivel 3: Si B tiene mixInPoints, usar seekTo al punto óptimo.
     */
    private fun triggerAutomixTransition() {
        if (isCrossFading) return
        // Velocidad estricta 1.0x durante Automix (sin pitch/tempo agresivo)
        enforceAutomixPlaybackSpeed()
        getNextPlayer()?.let { player ->
            val nextSong = MusicPlayerRemote.nextSong
            if (nextSong != null && nextSong != Song.emptySong) {
                nextDataSource = null
                setDataSourceImpl(player, nextSong.uri.toString()) { success ->
                    if (success) {
                        applyAutomixNextTrackSeek(player)
                        switchPlayer()
                    }
                }
            } else if (!nextDataSource.isNullOrEmpty()) {
                setDataSourceImpl(player, nextDataSource!!) { success ->
                    if (success) {
                        applyAutomixNextTrackSeek(player)
                        switchPlayer()
                    }
                    nextDataSource = null
                }
            } else {
                // Sin siguiente pista: permitir que onCompletion maneje el fin natural
                automixTransitionTriggered = false
            }
        } ?: run {
            automixTransitionTriggered = false
        }
    }

    /** Avanza al mix-in point de la canción B si lo tiene (Nivel 3). */
    private fun applyAutomixNextTrackSeek(player: MediaPlayer) {
        nextAutomixProfile?.mixInPoints?.firstOrNull { it > 0f }?.let { mixInSec ->
            try {
                player.seekTo((mixInSec * 1000).toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Consulta asíncronamente Room (Offline-First) para cueOutMs, outro_silence y fullProfileJson
     * de la pista actual (A) y la próxima (B). Llamado al cambiar de canción.
     */
    fun updateAutomixCueOut(songId: Long) {
        if (songId <= 0L) {
            resetAutomixState()
            return
        }
        automixLoadJob?.cancel()
        automixTransitionTriggered = false
        automixLoadJob = automixScope.launch {
            val nextSongId = MusicPlayerRemote.nextSong?.id ?: -1L
            
            data class AutoMixLoadResult(
                val cueOut: Long,
                val outroSilence: Long,
                val profA: code.name.monkey.retromusic.automix.AdvancedAutomixProfile?,
                val profB: code.name.monkey.retromusic.automix.AdvancedAutomixProfile?
            )

            val result = withContext(Dispatchers.IO) {
                try {
                    val repository: RoomRepository =
                        org.koin.java.KoinJavaComponent.get(RoomRepository::class.java)
                    
                    val entityA = repository.getAutomixDataBySongId(songId)
                    val entityB = if (nextSongId > 0) repository.getAutomixDataBySongId(nextSongId) else null
                    
                    val profA = code.name.monkey.retromusic.automix.AdvancedAutomixProfile.fromJson(entityA?.fullProfileJson)
                    val profB = code.name.monkey.retromusic.automix.AdvancedAutomixProfile.fromJson(entityB?.fullProfileJson)

                    AutoMixLoadResult(
                        entityA?.cueOutMs?.takeIf { it > 0L } ?: 0L,
                        entityA?.outroSilenceDurationMs?.takeIf { it > 0L } ?: 0L,
                        profA,
                        profB
                    )
                } catch (e: Exception) {
                    AutoMixLoadResult(0L, 0L, null, null)
                }
            }
            automixCueOutMs = result.cueOut
            automixOutroSilenceDurationMs = result.outroSilence
            currentAutomixProfile = result.profA
            nextAutomixProfile = result.profB
            
            if (PreferenceUtil.isAutomixEnabled) {
                enforceAutomixPlaybackSpeed()
            }
        }
    }

    private fun resetAutomixState() {
        automixLoadJob?.cancel()
        automixLoadJob = null
        automixCueOutMs = 0L
        automixOutroSilenceDurationMs = 0L
        currentAutomixProfile = null
        nextAutomixProfile = null
        automixTransitionTriggered = false
    }

    /** Mantiene pitch/tempo en 1.0x cuando Auto Mix está activo. */
    private fun enforceAutomixPlaybackSpeed() {
        try {
            getCurrentPlayer()?.setPlaybackSpeedPitch(AUTOMIX_PLAYBACK_SPEED, AUTOMIX_PLAYBACK_PITCH)
            getNextPlayer()?.let { next ->
                if (next.isPlaying) {
                    next.setPlaybackSpeedPitch(AUTOMIX_PLAYBACK_SPEED, AUTOMIX_PLAYBACK_PITCH)
                }
            }
        } catch (_: IllegalStateException) {
            // MediaPlayer aún no preparado
        }
    }

    private fun switchPlayer() {
        getNextPlayer()?.start()
        crossFade(getNextPlayer()!!, getCurrentPlayer()!!)
        currentPlayer =
            if (currentPlayer == CurrentPlayer.PLAYER_ONE || currentPlayer == CurrentPlayer.NOT_SET) {
                CurrentPlayer.PLAYER_TWO
            } else {
                CurrentPlayer.PLAYER_ONE
            }
        callbacks?.onTrackEndedWithCrossfade()
    }

    override fun setCrossFadeDuration(duration: Int) {
        crossFadeDuration = duration
    }

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        // Auto Mix: velocidad estrictamente 1.0x (ignora ajustes agresivos de tempo)
        val effectiveSpeed = if (PreferenceUtil.isAutomixEnabled) AUTOMIX_PLAYBACK_SPEED else speed
        val effectivePitch = if (PreferenceUtil.isAutomixEnabled) AUTOMIX_PLAYBACK_PITCH else pitch
        getCurrentPlayer()?.setPlaybackSpeedPitch(effectiveSpeed, effectivePitch)
        if (getNextPlayer()?.isPlaying == true) {
            getNextPlayer()?.setPlaybackSpeedPitch(effectiveSpeed, effectivePitch)
        }
    }

    private fun setDataSourceImpl(
        player: MediaPlayer,
        path: String,
        completion: (success: Boolean) -> Unit,
    ) {
        player.reset()
        try {
            if (path.startsWith("content://")) {
                player.setDataSource(context, path.toUri())
            } else {
                player.setDataSource(path)
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            val speed = if (PreferenceUtil.isAutomixEnabled) AUTOMIX_PLAYBACK_SPEED else playbackSpeed
            val pitch = if (PreferenceUtil.isAutomixEnabled) AUTOMIX_PLAYBACK_PITCH else playbackPitch
            player.playbackParams =
                PlaybackParams().setSpeed(speed).setPitch(pitch)

            player.setOnPreparedListener {
                player.setOnPreparedListener(null)
                completion(true)
            }
            player.prepare()
        } catch (e: Exception) {
            completion(false)
            e.printStackTrace()
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    companion object {
        val TAG: String = CrossFadePlayer::class.java.simpleName
        private const val AUTOMIX_PLAYBACK_SPEED = 1.0f
        private const val AUTOMIX_PLAYBACK_PITCH = 1.0f
    }
}

internal fun crossFadeScope(): CoroutineScope = CoroutineScope(Job() + Dispatchers.Default)

fun MediaPlayer.setPlaybackSpeedPitch(speed: Float, pitch: Float) {
    val wasPlaying = isPlaying
    playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch)
    if (!wasPlaying) {
        pause()
    }
}