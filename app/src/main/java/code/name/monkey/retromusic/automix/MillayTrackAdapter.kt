/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Engine
 */
package code.name.monkey.retromusic.automix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

/**
 * RecyclerView adapter for the MILLAY native search results.
 * Displays tracks fetched from Deezer's private API with glassmorphism cards.
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
        private val playBtn: ImageButton = itemView.findViewById(R.id.millayTrackPlay)
        private val downloadBtn: ImageButton = itemView.findViewById(R.id.millayTrackDownload)

        fun bind(track: DeezerTrack) {
            titleView.text = track.title
            artistView.text = track.artistName
            albumView.text = track.albumTitle
            durationView.text = track.durationString

            // Quality badge: show FLAC or 320 if available
            if (track.fileFlac > 0L || track.fileSize320 > 0L) {
                qualityBadge.text = track.qualityLabel
                qualityBadge.visibility = View.VISIBLE
            } else {
                qualityBadge.visibility = View.GONE
            }

            // Load album art with rounded corners using Glide
            Glide.with(artView.context)
                .load(track.coverUrlThumb)
                .apply(
                    RequestOptions()
                        .transform(RoundedCorners(20))
                        .placeholder(R.drawable.millay_art_placeholder)
                        .error(R.drawable.millay_art_placeholder)
                )
                .into(artView)

            // Play button: stream directly through Milla's player
            playBtn.setOnClickListener {
                onPlay(track)
            }

            // Tap anywhere on the card also plays
            itemView.setOnClickListener {
                onPlay(track)
            }

            // Download button: save to storage
            downloadBtn.setOnClickListener {
                onDownload(track)
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
