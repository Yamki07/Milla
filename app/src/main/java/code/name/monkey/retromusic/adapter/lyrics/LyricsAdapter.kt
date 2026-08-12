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
 * Implementa el Efecto Ola utilizando la vista personalizada SyllableLyricView,
 * transiciones suaves de escala (1.15f / 0.9f) y opacidad (1.0f / 0.4f) estilo Apple Music,
 * y desenfoque dinámico en líneas inactivas con RenderEffect en dispositivos Android 12+.
 */
class LyricsAdapter : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private var lyrics: List<LyricLine> = emptyList()
    var currentLineIndex: Int = -1
        private set

    var currentTimeMs: Long = 0L
        private set

    var currentTimeOffsetMs: Long = 0L

    var primaryColor: Int = Color.parseColor("#FF4081")
        private set

    var inactiveColor: Int = Color.parseColor("#66FFFFFF")
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

    private var attachedRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerView = null
    }

    fun updateTime(timeMs: Long) {
        this.currentTimeMs = timeMs + currentTimeOffsetMs
        if (currentLineIndex != -1 && currentLineIndex < lyrics.size) {
            // Se actualiza el holder activo en lugar de notificar (para evitar re-binds constantes que rompen animaciones de escala)
            val holder = attachedRecyclerView?.findViewHolderForAdapterPosition(currentLineIndex) as? LyricViewHolder
            holder?.updateProgress(this.currentTimeMs)
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
        holder.bind(line, position == currentLineIndex, position)
    }

    override fun getItemCount(): Int = lyrics.size

    inner class LyricViewHolder(val binding: ItemLyricLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < lyrics.size) {
                    onLyricLineClickListener?.invoke(lyrics[pos], pos)
                }
            }
        }

        fun bind(line: LyricLine, isActive: Boolean, position: Int) {
            binding.lyricText.setColors(primaryColor, inactiveColor)
            binding.lyricText.setLyricLine(line)
            binding.lyricText.animate().cancel()

            val isPast = !isActive && position < currentLineIndex
            val isUpcoming = !isActive && position == currentLineIndex + 1

            when {
                isActive -> {
                    // ACTIVE: scale(1.0) opacity(1) blur(0) — Monochrome .synced-line.active
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        binding.lyricText.setRenderEffect(null)
                    }
                    binding.lyricText.animate()
                        .scaleX(1.0f).scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(600L)
                        .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                        .start()
                }
                isUpcoming -> {
                    // UPCOMING: scale(0.98) opacity(0.7) blur(0.8px) — Monochrome .synced-line.upcoming
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        binding.lyricText.setRenderEffect(
                            RenderEffect.createBlurEffect(1.5f, 1.5f, Shader.TileMode.CLAMP)
                        )
                    }
                    binding.lyricText.animate()
                        .scaleX(0.98f).scaleY(0.98f)
                        .alpha(0.7f)
                        .setDuration(600L)
                        .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                        .start()
                }
                isPast -> {
                    // PAST: scale(0.93) opacity(0.3) blur(2px) — Monochrome .synced-line.past
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        binding.lyricText.setRenderEffect(
                            RenderEffect.createBlurEffect(4f, 4f, Shader.TileMode.CLAMP)
                        )
                    }
                    binding.lyricText.animate()
                        .scaleX(0.93f).scaleY(0.93f)
                        .alpha(0.3f)
                        .setDuration(600L)
                        .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                        .start()
                }
                else -> {
                    // DEFAULT INACTIVE: scale(0.95) opacity(0.5) blur(1.5px) — Monochrome .synced-line base
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        binding.lyricText.setRenderEffect(
                            RenderEffect.createBlurEffect(2.5f, 2.5f, Shader.TileMode.CLAMP)
                        )
                    }
                    binding.lyricText.animate()
                        .scaleX(0.95f).scaleY(0.95f)
                        .alpha(0.5f)
                        .setDuration(600L)
                        .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                        .start()
                }
            }
        }

        fun updateProgress(timeMs: Long) {
            binding.lyricText.updateTime(timeMs)
        }
    }
}
