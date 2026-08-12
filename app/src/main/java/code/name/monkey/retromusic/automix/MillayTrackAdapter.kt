/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Engine
 * Updated: Spotify-style UI with quality badge, heart, and more menu
 */
package code.name.monkey.retromusic.automix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

/**
 * RecyclerView adapter for the MILLAY native search results.
 * Spotify-style UI: quality badge · heart · download · three-dot menu.
 */
class MillayTrackAdapter(
    private val onPlay: (DeezerTrack) -> Unit,
    private val onDownload: (DeezerTrack) -> Unit
) : ListAdapter<DeezerTrack, MillayTrackAdapter.TrackViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_millay_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val artView: ImageView = itemView.findViewById(R.id.millayTrackArt)
        private val titleView: TextView = itemView.findViewById(R.id.millayTrackTitle)
        private val artistView: TextView = itemView.findViewById(R.id.millayTrackArtist)
        private val albumView: TextView = itemView.findViewById(R.id.millayTrackAlbum)
        private val durationView: TextView = itemView.findViewById(R.id.millayTrackDuration)
        private val qualityBadge: TextView = itemView.findViewById(R.id.millayTrackQualityBadge)
        private val qualityLabel: TextView = itemView.findViewById(R.id.millayTrackQualityLabel)
        private val likeBtn: ImageButton = itemView.findViewById(R.id.millayTrackLike)
        private val downloadBtn: ImageButton = itemView.findViewById(R.id.millayTrackDownload)
        private val moreBtn: ImageButton = itemView.findViewById(R.id.millayTrackMore)

        fun bind(track: DeezerTrack) {
            titleView.text = track.title
            artistView.text = track.artistName
            albumView.text = track.albumTitle
            durationView.text = track.durationString

            // Quality badge corner overlay (tiny FLAC/MP3 320)
            val badge = when {
                track.fileFlac > 0L -> "FLAC"
                track.fileSize320 > 0L -> "MP3 320"
                else -> ""
            }
            if (badge.isNotEmpty()) {
                qualityBadge.text = badge
                qualityBadge.visibility = View.VISIBLE
                // Also show the label above the title like Spotify (HD FLAC chip)
                qualityLabel.text = if (badge == "FLAC") "HD FLAC" else badge
                qualityLabel.visibility = View.VISIBLE
            } else {
                qualityBadge.visibility = View.GONE
                qualityLabel.visibility = View.GONE
            }

            // Album art
            Glide.with(artView.context)
                .load(track.coverUrlThumb)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.millay_art_placeholder)
                        .error(R.drawable.millay_art_placeholder)
                )
                .into(artView)

            // Tap row = play
            itemView.setOnClickListener { onPlay(track) }

            // Like button (toggle state)
            var liked = false
            likeBtn.setOnClickListener {
                liked = !liked
                likeBtn.setImageResource(
                    if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
                likeBtn.setColorFilter(
                    if (liked) 0xFF1DB954.toInt() else 0xB3FFFFFF.toInt()
                )
            }

            // Download button
            downloadBtn.setOnClickListener { onDownload(track) }

            // Three-dot more menu
            moreBtn.setOnClickListener { v ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add(0, 1, 0, "Descargar")
                popup.menu.add(0, 2, 1, "Agregar a Cola")
                popup.menu.add(0, 3, 2, "Ver Artista")
                popup.menu.add(0, 4, 3, "Compartir")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> { onDownload(track); true }
                        2 -> { Toast.makeText(v.context, "Agregado a cola: ${track.title}", Toast.LENGTH_SHORT).show(); true }
                        3 -> { Toast.makeText(v.context, "Artista: ${track.artistName}", Toast.LENGTH_SHORT).show(); true }
                        4 -> { Toast.makeText(v.context, "Compartiendo: ${track.title}", Toast.LENGTH_SHORT).show(); true }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeezerTrack>() {
            override fun areItemsTheSame(old: DeezerTrack, new: DeezerTrack) = old.id == new.id
            override fun areContentsTheSame(old: DeezerTrack, new: DeezerTrack) = old == new
        }
    }
}
