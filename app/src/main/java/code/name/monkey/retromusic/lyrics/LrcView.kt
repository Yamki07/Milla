/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.lyrics

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import androidx.core.content.ContextCompat
import androidx.core.graphics.withSave
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.util.LrcParser
import code.name.monkey.retromusic.util.LyricLine
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs

/**
 * 歌词 (Karaoke Syllable-by-Syllable Support)
 */
@SuppressLint("StaticFieldLeak")
class LrcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mLrcEntryList: MutableList<LyricViewLine> = ArrayList()
    private val mLrcPaint = TextPaint()
    private val mTimePaint = TextPaint()
    private var mTimeFontMetrics: Paint.FontMetrics? = null
    private var mPlayDrawable: Drawable? = null
    private var mDividerHeight = 0f
    private var mAnimationDuration: Long = 0
    private var mNormalTextColor = 0
    private var mNormalTextSize = 0f
    private var mCurrentTextColor = 0
    private var mCurrentTextSize = 0f
    private var mTimelineTextColor = 0
    private var mTimelineColor = 0
    private var mTimeTextColor = 0
    private var mDrawableWidth = 0
    private var mTimeTextWidth = 0
    private var mDefaultLabel: String? = null
    private var mLrcPadding = 0f
    private var mOnPlayClickListener: OnPlayClickListener? = null
    private var mAnimator: ValueAnimator? = null
    private var mGestureDetector: GestureDetector? = null
    private var mScroller: Scroller? = null
    private var mOffset = 0f
    private var mCurrentLine = 0
    private var isShowTimeline = false
    private var isTouching = false
    private var isFling = false
    private var mTextGravity = 0
    private var mCurrentTime = 0L

    private val hideTimelineRunnable = Runnable {
        if (hasLrc() && isShowTimeline) {
            isShowTimeline = false
            smoothScrollTo(mCurrentLine)
        }
    }

    private val viewScope = CoroutineScope(Dispatchers.Main + Job())

    private val mSimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if (hasLrc() && mOnPlayClickListener != null) {
                if (mOffset != getOffset(0)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                mScroller!!.forceFinished(true)
                removeCallbacks(hideTimelineRunnable)
                isTouching = true
                isShowTimeline = true
                invalidate()
                return true
            }
            return super.onDown(e)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (mOffset == getOffset(0) && distanceY < 0F) {
                return super.onScroll(e1, e2, distanceX, distanceY)
            }
            if (hasLrc()) {
                mOffset += -distanceY
                mOffset = mOffset.coerceAtMost(getOffset(0))
                mOffset = mOffset.coerceAtLeast(getOffset(mLrcEntryList.size - 1))
                invalidate()
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (hasLrc()) {
                mScroller!!.fling(
                    0, mOffset.toInt(), 0, velocityY.toInt(),
                    0, 0, getOffset(mLrcEntryList.size - 1).toInt(), getOffset(0).toInt()
                )
                isFling = true
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (hasLrc() && isShowTimeline && mPlayDrawable!!.bounds.contains(e.x.toInt(), e.y.toInt())) {
                val centerLineIdx = centerLine
                val centerLineTime = mLrcEntryList[centerLineIdx].time
                if (mOnPlayClickListener?.onPlayClick(centerLineTime) == true) {
                    isShowTimeline = false
                    removeCallbacks(hideTimelineRunnable)
                    mCurrentLine = centerLineIdx
                    animateCurrentTextSize()
                    return true
                }
            } else if (hasLrc() && mOnPlayClickListener != null) {
                val y = e.y - mOffset
                val tappedLine = getTappedLine(y)
                if (tappedLine in mLrcEntryList.indices) {
                    val tappedLineTime = mLrcEntryList[tappedLine].time
                    if (mOnPlayClickListener!!.onPlayClick(tappedLineTime)) {
                        isShowTimeline = false
                        removeCallbacks(hideTimelineRunnable)
                        mCurrentLine = tappedLine
                        smoothScrollTo(mCurrentLine)
                        invalidate()
                        return true
                    }
                }
            } else {
                callOnClick()
                return true
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private fun getTappedLine(y: Float): Int {
        var cumulativeHeight = 0f
        for (i in mLrcEntryList.indices) {
            val entry = mLrcEntryList[i]
            val lineHeight = entry.height.toFloat()
            val lineCenter = cumulativeHeight + (lineHeight / 2)
            if (abs(y - lineCenter) < lineHeight / 2) {
                return i
            }
            cumulativeHeight += lineHeight + mDividerHeight
        }
        return -1
    }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.LrcView)
        mCurrentTextSize = ta.getDimension(R.styleable.LrcView_lrcTextSize, resources.getDimension(R.dimen.lrc_current_text_size))
        mNormalTextSize = ta.getDimension(R.styleable.LrcView_lrcNormalTextSize, resources.getDimension(R.dimen.lrc_text_size))
        if (mNormalTextSize == 0f) mNormalTextSize = mCurrentTextSize
        mDividerHeight = ta.getDimension(R.styleable.LrcView_lrcDividerHeight, resources.getDimension(R.dimen.lrc_divider_height))
        
        val defDuration = resources.getInteger(R.integer.lrc_animation_duration)
        mAnimationDuration = ta.getInt(R.styleable.LrcView_lrcAnimationDuration, defDuration).toLong()
        if (mAnimationDuration < 0) mAnimationDuration = defDuration.toLong()

        mNormalTextColor = ta.getColor(R.styleable.LrcView_lrcNormalTextColor, ContextCompat.getColor(context, R.color.lrc_normal_text_color))
        mCurrentTextColor = ta.getColor(R.styleable.LrcView_lrcCurrentTextColor, ContextCompat.getColor(context, R.color.lrc_current_text_color))
        mTimelineTextColor = ta.getColor(R.styleable.LrcView_lrcTimelineTextColor, ContextCompat.getColor(context, R.color.lrc_timeline_text_color))
        mDefaultLabel = ta.getString(R.styleable.LrcView_lrcLabel) ?: context.getString(R.string.empty)
        mLrcPadding = ta.getDimension(R.styleable.LrcView_lrcPadding, 0f)
        mTimelineColor = ta.getColor(R.styleable.LrcView_lrcTimelineColor, ContextCompat.getColor(context, R.color.lrc_timeline_color))
        
        val timelineHeight = ta.getDimension(R.styleable.LrcView_lrcTimelineHeight, resources.getDimension(R.dimen.lrc_timeline_height))
        mPlayDrawable = ta.getDrawable(R.styleable.LrcView_lrcPlayDrawable) ?: ContextCompat.getDrawable(context, R.drawable.ic_play_arrow)
        mTimeTextColor = ta.getColor(R.styleable.LrcView_lrcTimeTextColor, ContextCompat.getColor(context, R.color.lrc_time_text_color))
        
        val timeTextSize = ta.getDimension(R.styleable.LrcView_lrcTimeTextSize, resources.getDimension(R.dimen.lrc_time_text_size))
        mTextGravity = ta.getInteger(R.styleable.LrcView_lrcTextGravity, 0) // 0=Center
        ta.recycle()
        
        mDrawableWidth = resources.getDimension(R.dimen.lrc_drawable_width).toInt()
        mTimeTextWidth = resources.getDimension(R.dimen.lrc_time_width).toInt()
        
        mLrcPaint.isAntiAlias = true
        mLrcPaint.textSize = mCurrentTextSize
        mLrcPaint.textAlign = Paint.Align.LEFT
        
        mTimePaint.isAntiAlias = true
        mTimePaint.textSize = timeTextSize
        mTimePaint.textAlign = Paint.Align.CENTER
        mTimePaint.strokeWidth = timelineHeight
        mTimePaint.strokeCap = Paint.Cap.ROUND
        mTimeFontMetrics = mTimePaint.fontMetrics
        
        mGestureDetector = GestureDetector(context, mSimpleOnGestureListener)
        mGestureDetector!!.setIsLongpressEnabled(false)
        mScroller = Scroller(context)
    }

    fun setNormalColor(normalColor: Int) {
        mNormalTextColor = normalColor
        postInvalidate()
    }

    fun setCurrentColor(currentColor: Int) {
        mCurrentTextColor = currentColor
        postInvalidate()
    }

    fun setTimelineTextColor(timelineTextColor: Int) {
        mTimelineTextColor = timelineTextColor
        postInvalidate()
    }

    fun setTimelineColor(timelineColor: Int) {
        mTimelineColor = timelineColor
        postInvalidate()
    }

    fun setTimeTextColor(timeTextColor: Int) {
        mTimeTextColor = timeTextColor
        postInvalidate()
    }

    fun setDraggable(draggable: Boolean, onPlayClickListener: OnPlayClickListener?) {
        mOnPlayClickListener = if (draggable) {
            requireNotNull(onPlayClickListener) { "if draggable == true, onPlayClickListener must not be null" }
            onPlayClickListener
        } else null
    }

    fun setLabel(label: String?) {
        runOnUi {
            mDefaultLabel = label
            invalidate()
        }
    }

    fun loadLrc(lrcFile: File) {
        runOnUi {
            reset()
            viewScope.launch(Dispatchers.IO) {
                val lines = LrcParser.parseSuspending(lrcFile)
                withContext(Dispatchers.Main) { onLrcLoaded(lines) }
            }
        }
    }

    fun loadLrc(lrcText: String?) {
        runOnUi {
            reset()
            if (lrcText.isNullOrBlank()) {
                onLrcLoaded(emptyList())
                return@runOnUi
            }
            viewScope.launch(Dispatchers.IO) {
                val lines = LrcParser.parseSuspending(lrcText)
                withContext(Dispatchers.Main) { onLrcLoaded(lines) }
            }
        }
    }

    fun hasLrc(): Boolean = mLrcEntryList.isNotEmpty()

    fun updateTime(time: Long) {
        runOnUi {
            mCurrentTime = time
            if (!hasLrc()) return@runOnUi
            
            val line = findShowLine(time + 300L)
            if (line != mCurrentLine) {
                mCurrentLine = line
                if (!isShowTimeline) {
                    smoothScrollTo(line)
                    animateCurrentTextSize()
                } else {
                    invalidate()
                }
            } else {
                // Invalidate for syllable highlighting updates
                invalidate()
            }
        }
    }
    
    @Deprecated("Use updateTime instead")
    fun onDrag(time: Long) {
        updateTime(time)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            initPlayDrawable()
            initEntryList()
            if (hasLrc()) {
                smoothScrollTo(mCurrentLine, 0L)
            }
        }
    }

    @Suppress("Deprecation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2

        if (!hasLrc()) {
            mLrcPaint.color = mCurrentTextColor
            mLrcPaint.shader = null
            val staticLayout = StaticLayout(
                mDefaultLabel, mLrcPaint, lrcWidth.toInt(),
                Layout.Alignment.ALIGN_CENTER, 1f, 0f, false
            )
            drawText(canvas, staticLayout, centerY.toFloat())
            return
        }

        val centerLineIdx = centerLine
        if (isShowTimeline) {
            mPlayDrawable?.draw(canvas)
            mTimePaint.color = mTimeTextColor
            
            val ms = mLrcEntryList[centerLineIdx].time
            val timeText = String.format("%02d:%02d", (ms / 60000), (ms % 60000) / 1000)
            val timeX = (width - mTimeTextWidth / 2).toFloat()
            val timeY = centerY - (mTimeFontMetrics!!.descent + mTimeFontMetrics!!.ascent) / 2
            canvas.drawText(timeText, timeX, timeY, mTimePaint)
        }

        canvas.translate(0f, mOffset)
        var y = 0f
        for (i in mLrcEntryList.indices) {
            if (i > 0) {
                y += ((mLrcEntryList[i - 1].height + mLrcEntryList[i].height) shr 1) + mDividerHeight
            }
            
            val viewLine = mLrcEntryList[i]
            
            if (i == mCurrentLine) {
                mLrcPaint.textSize = mCurrentTextSize
                
                // Syllable Karaoke Effect
                val progress = getHighlightProgress(viewLine.lyricLine, mCurrentTime, mLrcPaint)
                if (progress > 0f && progress < 1f) {
                    val lineLeft = viewLine.staticLayout.getLineLeft(0)
                    val lineWidth = viewLine.staticLayout.getLineWidth(0)
                    val splitX = lineLeft + lineWidth * progress
                    
                    val shader = LinearGradient(
                        lineLeft, 0f, lineLeft + lineWidth, 0f,
                        intArrayOf(mCurrentTextColor, mCurrentTextColor, mNormalTextColor, mNormalTextColor),
                        floatArrayOf(0f, progress, progress, 1f),
                        Shader.TileMode.CLAMP
                    )
                    mLrcPaint.shader = shader
                } else {
                    mLrcPaint.shader = null
                    mLrcPaint.color = if (progress >= 1f) mCurrentTextColor else mNormalTextColor
                }
            } else if (isShowTimeline && i == centerLineIdx) {
                mLrcPaint.shader = null
                mLrcPaint.textSize = mNormalTextSize
                mLrcPaint.color = mTimelineTextColor
            } else {
                mLrcPaint.shader = null
                mLrcPaint.textSize = mNormalTextSize
                mLrcPaint.color = mNormalTextColor
            }
            drawText(canvas, viewLine.staticLayout, y)
        }
    }
    
    private fun getHighlightProgress(line: LyricLine, time: Long, paint: TextPaint): Float {
        if (line.syllables.isEmpty()) {
            return if (time >= line.timeMs) 1.0f else 0.0f
        }
        if (time < line.syllables.first().startMs) return 0f
        val lastSyl = line.syllables.last()
        if (time > lastSyl.startMs + lastSyl.durationMs) return 1f

        var playedWidth = 0f
        var totalWidth = 0f
        for (syl in line.syllables) {
            val w = paint.measureText(syl.text)
            totalWidth += w
            if (time >= syl.startMs + syl.durationMs) {
                playedWidth += w
            } else if (time >= syl.startMs) {
                val progress = (time - syl.startMs).toFloat() / syl.durationMs.coerceAtLeast(1L)
                playedWidth += w * progress
            }
        }
        return if (totalWidth > 0) playedWidth / totalWidth else 0f
    }

    private fun drawText(canvas: Canvas, staticLayout: StaticLayout, y: Float) {
        canvas.withSave {
            translate(mLrcPadding, y - (staticLayout.height shr 1))
            staticLayout.draw(this)
        }
    }

    fun animateCurrentTextSize() {
        val currentTextSize = mCurrentTextSize
        ValueAnimator.ofFloat(mNormalTextSize, currentTextSize).apply {
            addUpdateListener {
                mCurrentTextSize = it.animatedValue as Float
                invalidate()
            }
            duration = mAnimationDuration
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return mGestureDetector!!.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            mOffset = mScroller!!.currY.toFloat()
            invalidate()
        }
        if (isFling && mScroller!!.isFinished) {
            isFling = false
            if (hasLrc() && !isTouching) {
                adjustCenter()
                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideTimelineRunnable)
        viewScope.cancel()
        super.onDetachedFromWindow()
    }

    @Suppress("Deprecation")
    private fun onLrcLoaded(lines: List<LyricLine>) {
        mLrcEntryList.clear()
        if (lines.isNotEmpty()) {
            val align = when (mTextGravity) {
                1 -> Layout.Alignment.ALIGN_NORMAL
                2 -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_CENTER
            }
            for (line in lines) {
                val layout = StaticLayout(line.text, mLrcPaint, lrcWidth.toInt(), align, 1f, 0f, false)
                mLrcEntryList.add(LyricViewLine(line, layout))
            }
        }
        mLrcEntryList.sort()
        initEntryList()
        invalidate()
    }

    private fun initPlayDrawable() {
        val l = (mTimeTextWidth - mDrawableWidth) / 2
        val t = height / 2 - mDrawableWidth / 2
        mPlayDrawable?.setBounds(l, t, l + mDrawableWidth, t + mDrawableWidth)
    }

    @Suppress("Deprecation")
    private fun initEntryList() {
        if (!hasLrc() || width == 0) return
        val align = when (mTextGravity) {
            1 -> Layout.Alignment.ALIGN_NORMAL
            2 -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        for (i in mLrcEntryList.indices) {
            val viewLine = mLrcEntryList[i]
            val layout = StaticLayout(viewLine.lyricLine.text, mLrcPaint, lrcWidth.toInt(), align, 1f, 0f, false)
            mLrcEntryList[i] = LyricViewLine(viewLine.lyricLine, layout)
        }
        mOffset = (height / 2).toFloat()
    }

    fun reset() {
        endAnimation()
        mScroller!!.forceFinished(true)
        isShowTimeline = false
        isTouching = false
        isFling = false
        removeCallbacks(hideTimelineRunnable)
        mLrcEntryList.clear()
        mOffset = 0f
        mCurrentLine = 0
        invalidate()
    }

    private fun adjustCenter() {
        smoothScrollTo(centerLine, ADJUST_DURATION)
    }

    private fun smoothScrollTo(line: Int, duration: Long = mAnimationDuration) {
        val targetOffset = getOffset(line)
        endAnimation()
        mAnimator = ValueAnimator.ofFloat(mOffset, targetOffset).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                mOffset = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun endAnimation() {
        if (mAnimator?.isRunning == true) {
            mAnimator!!.end()
        }
    }

    private fun findShowLine(time: Long): Int {
        var left = 0
        var right = mLrcEntryList.size - 1
        while (left <= right) {
            val middle = (left + right) / 2
            val middleTime = mLrcEntryList[middle].time
            if (time < middleTime) {
                right = middle - 1
            } else {
                if (middle + 1 >= mLrcEntryList.size || time < mLrcEntryList[middle + 1].time) {
                    return middle
                }
                left = middle + 1
            }
        }
        return 0
    }

    private val centerLine: Int
        get() {
            var centerIdx = 0
            var minDistance = Float.MAX_VALUE
            for (i in mLrcEntryList.indices) {
                val dist = abs(mOffset - getOffset(i))
                if (dist < minDistance) {
                    minDistance = dist
                    centerIdx = i
                }
            }
            return centerIdx
        }

    private fun getOffset(line: Int): Float {
        if (mLrcEntryList.isEmpty()) return 0F
        if (mLrcEntryList[line].offset == Float.MIN_VALUE) {
            var offset = (height / 2).toFloat()
            for (i in 1..line) {
                offset -= ((mLrcEntryList[i - 1].height + mLrcEntryList[i].height) shr 1) + mDividerHeight
            }
            mLrcEntryList[line].offset = offset
        }
        return mLrcEntryList[line].offset
    }

    private val lrcWidth: Float get() = width - mLrcPadding * 2

    private fun runOnUi(r: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run() else post(r)
    }

    fun interface OnPlayClickListener {
        fun onPlayClick(time: Long): Boolean
    }

    companion object {
        private const val ADJUST_DURATION: Long = 100
        private const val TIMELINE_KEEP_TIME = 4 * DateUtils.SECOND_IN_MILLIS
    }
}
