package code.name.monkey.retromusic.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import code.name.monkey.retromusic.lyrics.SyncedLyricLine
import code.name.monkey.retromusic.lyrics.SyncedLyricsParser
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Superficie de letras de bajo coste: dibuja solo una ventana de líneas y usa Choreographer.
 * La actualización se limita a 60 FPS aunque el panel físico admita una frecuencia mayor.
 */
class SyncedLyricsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private var lines: List<SyncedLyricLine> = emptyList()
    private var positionProvider: (() -> Long)? = null
    private var playingProvider: (() -> Boolean)? = null
    private var onLineClick: ((Long) -> Unit)? = null
    private var currentIndex = -1
    private var visibleCenter = -1f
    private var animationFrom = -1f
    private var animationTo = -1f
    private var animationStartedNs = 0L
    private var lastFrameNs = 0L
    private var isFrameLoopRunning = false
    private var emptyLabel = "No hay letras sincronizadas"
    private var activeColor = Color.WHITE
    private var mutedColor = Color.argb(110, 255, 255, 255)

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameNs: Long) {
            if (!isAttachedToWindow || !isShown) { isFrameLoopRunning = false; return }
            if (lastFrameNs == 0L || frameNs - lastFrameNs >= FRAME_INTERVAL_NS) {
                lastFrameNs = frameNs
                updateFrame(frameNs)
            }
            if (playingProvider?.invoke() == true || animationStartedNs > 0L) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                isFrameLoopRunning = false
            }
        }
    }

    fun setPositionSource(position: () -> Long, isPlaying: () -> Boolean) {
        positionProvider = position; playingProvider = isPlaying; startFrameLoop()
    }
    /** Llamado por el callback de progreso real para reanudar el bucle al volver a reproducir. */
    fun refreshPlayback() = startFrameLoop()
    fun setOnLineClickListener(listener: (Long) -> Unit) { onLineClick = listener }
    fun setPalette(current: Int, muted: Int) { activeColor = current; mutedColor = muted; invalidate() }
    fun setEmptyLabel(label: String) { emptyLabel = label; invalidate() }

    fun submitLines(newLines: List<SyncedLyricLine>) {
        lines = newLines
        currentIndex = -1
        visibleCenter = -1f
        invalidate()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startFrameLoop() }
    override fun onDetachedFromWindow() { stopFrameLoop(); super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) { super.onVisibilityChanged(changedView, visibility); if (visibility == VISIBLE) startFrameLoop() else stopFrameLoop() }

    private fun startFrameLoop() {
        if (!isFrameLoopRunning && isAttachedToWindow) { isFrameLoopRunning = true; lastFrameNs = 0L; Choreographer.getInstance().postFrameCallback(frameCallback) }
    }
    private fun stopFrameLoop() { if (isFrameLoopRunning) Choreographer.getInstance().removeFrameCallback(frameCallback); isFrameLoopRunning = false }

    private fun updateFrame(frameNs: Long) {
        val index = SyncedLyricsParser.currentLineIndex(lines, positionProvider?.invoke()?.coerceAtLeast(0L) ?: 0L)
        if (index != currentIndex) {
            currentIndex = index
            animationFrom = if (visibleCenter < 0f) index.toFloat() else visibleCenter
            animationTo = index.toFloat()
            animationStartedNs = frameNs
        }
        if (animationStartedNs > 0L) {
            val progress = ((frameNs - animationStartedNs).toFloat() / LINE_TRANSITION_NS).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress) * (1f - progress)
            visibleCenter = animationFrom + (animationTo - animationFrom) * eased
            if (progress >= 1f) animationStartedNs = 0L
        }
        if (playingProvider?.invoke() == true || animationStartedNs > 0L) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lines.isEmpty()) { drawCentered(canvas, emptyLabel, height * 0.5f, mutedPaint.apply { color = mutedColor; textSize = sp(16f); alpha = 180 }); return }
        val center = if (visibleCenter >= 0f) visibleCenter else currentIndex.coerceAtLeast(0).toFloat()
        val anchorY = height * 0.43f
        val gap = sp(52f)
        val nearest = center.roundToInt()
        val start = (nearest - WINDOW_LINES).coerceAtLeast(0)
        val end = (nearest + WINDOW_LINES).coerceAtMost(lines.lastIndex)
        for (index in start..end) {
            val distance = index - center
            val y = anchorY + distance * gap
            if (y < -gap || y > height + gap) continue
            val isActive = index == currentIndex
            val proximity = (1f - abs(distance) / (WINDOW_LINES + 1f)).coerceIn(0f, 1f)
            val paint = if (isActive) activePaint else mutedPaint
            paint.color = if (isActive) activeColor else mutedColor
            paint.textSize = sp(if (isActive) 28f else 23f)
            paint.alpha = if (isActive) 255 else (45 + 145 * proximity).roundToInt()
            drawCentered(canvas, lines[index].text, y, paint)
        }
    }

    private fun drawCentered(canvas: Canvas, text: String, centerY: Float, paint: Paint) {
        val blocks = text.split('\n').filter { it.isNotBlank() }
        val lineHeight = paint.fontMetrics.run { bottom - top }
        val top = centerY - (blocks.size - 1) * lineHeight / 2f
        blocks.forEachIndexed { index, line -> canvas.drawText(line, width / 2f, top + index * lineHeight - paint.fontMetrics.ascent, paint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && lines.isNotEmpty()) {
            val gap = sp(52f)
            val center = if (visibleCenter >= 0f) visibleCenter else currentIndex.coerceAtLeast(0).toFloat()
            val target = (center + (event.y - height * 0.43f) / gap).roundToInt().coerceIn(0, lines.lastIndex)
            onLineClick?.invoke(lines[target].startTimeMs)
            performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private companion object {
        const val WINDOW_LINES = 7
        const val FRAME_INTERVAL_NS = 16_666_667L
        const val LINE_TRANSITION_NS = 260_000_000L
    }
}
