package code.name.monkey.retromusic.service

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.provider.Settings
import androidx.core.animation.doOnEnd
import code.name.monkey.retromusic.service.playback.Playback
import code.name.monkey.retromusic.util.PreferenceUtil

class AudioFader {
    companion object {
        fun startFadeAnimator(
            playback: Playback,
            fadeIn: Boolean, /* fadeIn -> true  fadeOut -> false*/
            callback: Runnable? = null, /* Code to run when Animator Ends*/
        ) {
            val duration = PreferenceUtil.audioFadeDuration.toLong()
            if (duration == 0L) {
                callback?.run()
                return
            }
            val startValue = if (fadeIn) 0f else 1.0f
            val endValue = if (fadeIn) 1.0f else 0f
            val animator = ValueAnimator.ofFloat(startValue, endValue)
            animator.duration = duration
            animator.addUpdateListener { animation: ValueAnimator ->
                playback.setVolume(animation.animatedValue as Float)
            }
            animator.doOnEnd {
                callback?.run()
            }
            animator.start()
        }

        fun createFadeAnimator(
            player: MediaPlayer,
            fadeIn: Boolean,
            duration: Long,
            onEnd: (() -> Unit)? = null
        ): Animator {
            val startValue = if (fadeIn) 0f else 1.0f
            val endValue = if (fadeIn) 1.0f else 0f
            val animator = ValueAnimator.ofFloat(startValue, endValue)
            animator.duration = duration
            animator.addUpdateListener { animation: ValueAnimator ->
                try {
                    val vol = animation.animatedValue as Float
                    player.setVolume(vol, vol)
                } catch (e: Exception) {
                }
            }
            animator.doOnEnd {
                onEnd?.invoke()
            }
            return animator
        }

        fun createCrossFadeAnimator(
            fadeInMp: MediaPlayer,
            fadeOutMp: MediaPlayer,
            duration: Long,
            onEnd: (() -> Unit)? = null
        ): Animator {
            val animator = ValueAnimator.ofFloat(0f, 1.0f)
            animator.duration = duration
            animator.addUpdateListener { animation: ValueAnimator ->
                try {
                    val vol = animation.animatedValue as Float
                    fadeInMp.setVolume(vol, vol)
                    fadeOutMp.setVolume(1.0f - vol, 1.0f - vol)
                } catch (e: Exception) {
                }
            }
            animator.doOnEnd {
                onEnd?.invoke()
            }
            return animator
        }
    }
}