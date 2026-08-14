package code.name.monkey.retromusic.views

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.PlaybackOrchestrator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/** Renderiza únicamente datos recibidos del crossfade real; no inicia ni simula transiciones de audio. */
class AutoMixTransitionAnimator(
    private val background: View,
    private val outgoingCover: ImageView,
    private val incomingCover: ImageView,
) {
    private val colorEvaluator = ArgbEvaluator()
    private var outgoingColor = Color.rgb(16, 24, 32)
    private var incomingColor = Color.rgb(24, 32, 40)
    private var preparedPair: Pair<Long, Long>? = null
    private var lastState = PlaybackOrchestrator.AutoMixTransitionState()

    fun render(state: PlaybackOrchestrator.AutoMixTransitionState) {
        lastState = state
        if (!state.isRunning || state.outgoing == null || state.incoming == null) {
            reset()
            return
        }
        val pair = state.outgoing.id to state.incoming.id
        if (preparedPair != pair) {
            preparedPair = pair
            loadCover(state.outgoing, outgoingCover, true)
            loadCover(state.incoming, incomingCover, false)
        }
        val progress = state.progress.coerceIn(0f, 1f)
        background.isVisible = true
        outgoingCover.isVisible = true
        incomingCover.isVisible = true
        outgoingCover.apply {
            alpha = 1f - progress
            scaleX = 1f - 0.4f * progress
            scaleY = 1f - 0.4f * progress
            translationZ = -8f * progress
        }
        incomingCover.apply {
            alpha = progress
            scaleX = 0.6f + 0.4f * progress
            scaleY = 0.6f + 0.4f * progress
            translationZ = 8f * progress
        }
        background.setBackgroundColor(colorEvaluator.evaluate(progress, outgoingColor, incomingColor) as Int)
    }

    private fun loadCover(song: Song, targetView: ImageView, isOutgoing: Boolean) {
        Glide.with(targetView)
            .asBitmap()
            .load(RetroGlideExtension.getSongModel(song))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    targetView.setImageBitmap(resource)
                    val color = Palette.from(resource).generate().getDominantColor(if (isOutgoing) outgoingColor else incomingColor)
                    if (isOutgoing) outgoingColor = color else incomingColor = color
                    render(lastState)
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) = Unit
            })
    }

    private fun reset() {
        background.isVisible = false
        outgoingCover.isVisible = false
        incomingCover.isVisible = false
        outgoingCover.apply { alpha = 1f; scaleX = 1f; scaleY = 1f; translationZ = 0f }
        incomingCover.apply { alpha = 0f; scaleX = 0.6f; scaleY = 0.6f; translationZ = 0f }
        preparedPair = null
    }
}
