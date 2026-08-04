/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.adapter.lyrics

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemLyricLineBinding
import code.name.monkey.retromusic.util.LyricLine

/**
 * Adapter inmersivo para letras sincronizadas (Karaoke 2.0).
 * Implementa el Efecto Ola de BetterLyrics (LinearGradient con Matrix + ValueAnimator),
 * transiciones suaves de escala (1.15f / 0.9f) y opacidad (1.0f / 0.4f) estilo Apple Music,
 * y desenfoque dinámico en líneas inactivas con RenderEffect en dispositivos Android 12+.
 */
class LyricsAdapter : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private var lyrics: List<LyricLine> = emptyList()
    var currentLineIndex: Int = -1
        private set

    var primaryColor: Int = Color.parseColor("#FF4081")
        private set

    var onLyricLineClickListener: ((LyricLine, Int) -> Unit)? = null

    fun submitList(newList: List<LyricLine>) {
        lyrics = newList
        currentLineIndex = -1
        notifyDataSetChanged()
    }

    fun setCurrentLineIndex(newIndex: Int) {
        if (newIndex != currentLineIndex && newIndex in lyrics.indices) {
            val oldIndex = currentLineIndex
            currentLineIndex = newIndex
            if (oldIndex != -1 && oldIndex < lyrics.size) {
                notifyItemChanged(oldIndex)
            }
            if (newIndex in lyrics.indices) {
                notifyItemChanged(newIndex)
            }
        }
    }

    fun setWaveColor(color: Int) {
        if (this.primaryColor != color) {
            this.primaryColor = color
            if (currentLineIndex != -1 && currentLineIndex < lyrics.size) {
                notifyItemChanged(currentLineIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val binding = ItemLyricLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LyricViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val line = lyrics[position]
        holder.bind(line, position == currentLineIndex)
    }

    override fun getItemCount(): Int = lyrics.size

    override fun onViewRecycled(holder: LyricViewHolder) {
        super.onViewRecycled(holder)
        holder.stopWaveEffect()
    }

    inner class LyricViewHolder(val binding: ItemLyricLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var waveAnimator: ValueAnimator? = null

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < lyrics.size) {
                    onLyricLineClickListener?.invoke(lyrics[pos], pos)
                }
            }
        }

        fun bind(line: LyricLine, isActive: Boolean) {
            binding.lyricText.text = line.text
            binding.lyricText.animate().cancel()

            if (isActive) {
                // LÍNEA ACTIVA: Escala 1.15f, Alfa 1.0f, sin difuminado, con Efecto Ola en shader
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.lyricText.setRenderEffect(null)
                }

                binding.lyricText.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(1.0f)
                    .setDuration(280L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                startWaveEffect()
            } else {
                // LÍNEA INACTIVA: Escala 0.9f, Alfa 0.4f, difuminado suave si API >= 31, sin shader
                stopWaveEffect()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.lyricText.setRenderEffect(
                        RenderEffect.createBlurEffect(3.5f, 3.5f, Shader.TileMode.CLAMP)
                    )
                }

                binding.lyricText.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0.4f)
                    .setDuration(280L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        private fun startWaveEffect() {
            stopWaveEffect()
            val textView = binding.lyricText
            val width = if (textView.width > 0) textView.width.toFloat() else 800f
            val colors = intArrayOf(Color.WHITE, primaryColor, Color.WHITE)
            val positions = floatArrayOf(0f, 0.5f, 1f)
            val gradient = LinearGradient(
                0f, 0f, width * 1.5f, 0f,
                colors,
                positions,
                Shader.TileMode.MIRROR
            )
            textView.paint.shader = gradient
            val matrix = Matrix()

            waveAnimator = ValueAnimator.ofFloat(0f, width * 2f).apply {
                duration = 2200L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    val translate = animation.animatedValue as Float
                    matrix.setTranslate(translate, 0f)
                    gradient.setLocalMatrix(matrix)
                    textView.invalidate()
                }
                start()
            }
        }

        fun stopWaveEffect() {
            waveAnimator?.let {
                it.cancel()
            }
            waveAnimator = null
            binding.lyricText.paint.shader = null
            binding.lyricText.invalidate()
        }
    }
}
