/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.views

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
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
            updateGradient()
        }
    }

    private fun updateGradient() {
        val line = lyricLine ?: return
        val currentLayout = layout ?: return

        if (line.syllables.isEmpty()) {
            // Classic mode: no syllables
            paint.shader = null
            setTextColor(if (currentTimeMs >= line.timeMs) activeColor else inactiveColor)
            invalidate()
            return
        }

        // Syllable mode: find the active syllable
        var progressX = 0f
        var isBeforeFirst = true
        var isAfterLast = true

        var startIndex = 0
        for (syllable in line.syllables) {
            val startMs = syllable.startMs
            val endMs = startMs + syllable.durationMs
            val length = syllable.text.length

            if (currentTimeMs < startMs) {
                // Not yet reached this syllable
                isAfterLast = false
                break
            } else if (currentTimeMs > endMs) {
                // Passed this syllable
                isBeforeFirst = false
                startIndex += length
                // Default progressX to end of this syllable in case it's the last one we passed
                val endOffset = startIndex
                if (endOffset <= text.length) {
                    progressX = currentLayout.getPrimaryHorizontal(endOffset)
                }
            } else {
                // Inside this syllable!
                isBeforeFirst = false
                isAfterLast = false
                
                val startX = currentLayout.getPrimaryHorizontal(startIndex)
                val endOffset = startIndex + length
                val endX = if (endOffset <= text.length) {
                    currentLayout.getPrimaryHorizontal(endOffset)
                } else {
                    startX // Fallback
                }
                
                val fraction = (currentTimeMs - startMs).toFloat() / syllable.durationMs.toFloat()
                progressX = startX + (endX - startX) * fraction
                break
            }
        }

        if (isBeforeFirst) {
            paint.shader = null
            setTextColor(inactiveColor)
        } else if (isAfterLast) {
            paint.shader = null
            setTextColor(activeColor)
        } else {
            // Add padding offset since getPrimaryHorizontal is relative to the text layout
            val xOffset = paddingLeft.toFloat()
            val totalProgressX = progressX + xOffset

            // Create a sharp linear gradient at totalProgressX
            val shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(activeColor, activeColor, inactiveColor, inactiveColor),
                floatArrayOf(0f, totalProgressX / width.toFloat(), (totalProgressX + 0.001f) / width.toFloat(), 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            // Need to call setTextColor to apply shader correctly in some devices, though the color is ignored
            setTextColor(Color.WHITE) 
        }

        invalidate()
    }
}
