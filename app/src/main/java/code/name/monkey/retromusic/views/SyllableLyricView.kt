/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Region
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import code.name.monkey.retromusic.util.LyricLine

/**
 * Custom TextView that renders syllables with a dynamic wave effect (Karaoke).
 */
class SyllableLyricView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var lyricLine: LyricLine? = null
    private var currentTimeMs: Long = 0L

    private var activeColor: Int = Color.WHITE
    private var inactiveColor: Int = Color.parseColor("#66FFFFFF") // 40% white

    fun setColors(active: Int, inactive: Int) {
        this.activeColor = active
        this.inactiveColor = inactive
        invalidate()
    }

    fun setLyricLine(line: LyricLine?) {
        this.lyricLine = line
        this.text = line?.text ?: ""
        // Reset shader
        paint.shader = null
        invalidate()
    }

    fun updateTime(timeMs: Long) {
        if (this.currentTimeMs != timeMs) {
            this.currentTimeMs = timeMs
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val line = lyricLine
        if (line == null || line.syllables.isEmpty()) {
            // Classic mode: no syllables
            val isActive = line != null && currentTimeMs >= line.timeMs
            setTextColor(if (isActive) activeColor else inactiveColor)
            super.onDraw(canvas)
            return
        }

        val currentLayout = layout ?: return

        // 1. Draw inactive text completely
        setTextColor(inactiveColor)
        super.onDraw(canvas)

        // 2. Determine active text bounds
        var isBeforeFirst = true
        var isAfterLast = true
        var activeSyllableStartIndex = 0
        var activeSyllableFraction = 0f

        var startIndex = 0
        for (syllable in line.syllables) {
            val startMs = syllable.startMs
            val endMs = startMs + syllable.durationMs
            val length = syllable.text.length

            if (currentTimeMs < startMs) {
                isAfterLast = false
                break
            } else if (currentTimeMs > endMs) {
                isBeforeFirst = false
                startIndex += length
            } else {
                isBeforeFirst = false
                isAfterLast = false
                activeSyllableStartIndex = startIndex
                
                val fraction = (currentTimeMs - startMs).toFloat() / syllable.durationMs.toFloat()
                activeSyllableFraction = fraction.coerceIn(0f, 1f)
                break
            }
        }

        if (isBeforeFirst) return

        if (isAfterLast) {
            setTextColor(activeColor)
            super.onDraw(canvas)
            return
        }

        // 3. Draw active text clipped perfectly to syllables (even across line breaks)
        
        // A) Draw previously completed syllables completely
        if (activeSyllableStartIndex > 0) {
            canvas.save()
            val completedPath = android.graphics.Path()
            currentLayout.getSelectionPath(0, activeSyllableStartIndex, completedPath)
            completedPath.offset(paddingLeft.toFloat(), paddingTop.toFloat())
            canvas.clipPath(completedPath)
            setTextColor(activeColor)
            super.onDraw(canvas)
            canvas.restore()
        }
        
        // B) Draw current active syllable with scale effect (Wave / Pop)
        if (activeSyllableFraction > 0f) {
            val activePath = android.graphics.Path()
            val syllableLength = line.syllables.find { it.startMs <= currentTimeMs && it.startMs + it.durationMs >= currentTimeMs }?.text?.length ?: 0
            
            if (syllableLength > 0) {
                val startOffset = activeSyllableStartIndex
                val endOffset = startOffset + syllableLength
                
                val startLine = currentLayout.getLineForOffset(startOffset)
                val endLine = currentLayout.getLineForOffset(endOffset)
                
                if (startLine == endLine) {
                    val startX = currentLayout.getPrimaryHorizontal(startOffset)
                    val endX = currentLayout.getPrimaryHorizontal(endOffset)
                    val currentX = startX + (endX - startX) * activeSyllableFraction
                    
                    activePath.addRect(
                        startX,
                        currentLayout.getLineTop(startLine).toFloat(),
                        currentX,
                        currentLayout.getLineBottom(startLine).toFloat(),
                        android.graphics.Path.Direction.CW
                    )
                } else {
                    val fractionIndex = startOffset + (syllableLength * activeSyllableFraction).toInt()
                    currentLayout.getSelectionPath(startOffset, fractionIndex, activePath)
                }
                
                activePath.offset(paddingLeft.toFloat(), paddingTop.toFloat())
                
                canvas.save()
                
                // Calculate bounding box for pivot
                val bounds = android.graphics.RectF()
                activePath.computeBounds(bounds, true)
                
                // Efecto Ola: Escala basada en la fracción (hace una onda sinoidal que sube y baja ligeramente)
                // Se eleva hasta un 8% más grande en el medio de la sílaba
                val scale = 1f + 0.08f * Math.sin(activeSyllableFraction * Math.PI).toFloat()
                canvas.scale(scale, scale, bounds.centerX(), bounds.centerY())
                
                canvas.clipPath(activePath)
                setTextColor(activeColor)
                super.onDraw(canvas)
                
                canvas.restore()
            }
        }
    }
}
