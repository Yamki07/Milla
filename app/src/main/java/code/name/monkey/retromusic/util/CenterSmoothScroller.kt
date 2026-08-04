/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.util

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 * Desplazamiento orgánico y centrado perfecto al estilo am-lyrics (Apple Music).
 */
class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {

    companion object {
        const val SNAP_TO_CENTER = 0
    }

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        // Fuerza el centrado perfecto del elemento en relación con la vista del RecyclerView
        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
    }

    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        // Scroll más lento y orgánico (estilo Apple Music)
        return 150f / displayMetrics.densityDpi
    }

    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_CENTER
    }
}
