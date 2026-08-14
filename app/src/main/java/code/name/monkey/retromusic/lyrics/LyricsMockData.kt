package code.name.monkey.retromusic.lyrics

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import code.name.monkey.retromusic.views.SyncedLyricsView

/** Datos de desarrollo que permiten evaluar LRC mejorado y karaoke sin fuente de audio. */
object LyricsMockData {
    const val ENHANCED_LRC = """
[00:00.00] <00:00.00>Welcome <00:00.75>to <00:01.10>the <00:01.45>Milla <00:02.00>night
[00:03.00] <00:03.00>Every <00:03.55>word <00:04.00>moves <00:04.50>with <00:04.85>the <00:05.20>beat
[00:06.50] <00:06.50>Auto <00:07.00>Mix <00:07.40>keeps <00:07.85>the <00:08.20>floor <00:08.60>alive
[00:10.00] <00:10.00>Fine <00:10.50>tune <00:10.95>the <00:11.25>lyrics <00:11.90>by <00:12.20>five <00:12.60>hundred
""".trimIndent()

    fun startPreview(view: SyncedLyricsView): ValueAnimator = ValueAnimator.ofInt(0, 14_000).apply {
        duration = 14_000L
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { view.setPreviewPosition((it.animatedValue as Int).toLong()) }
        start()
    }
}
