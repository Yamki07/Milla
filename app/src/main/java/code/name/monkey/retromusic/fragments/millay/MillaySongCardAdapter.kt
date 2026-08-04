/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.simpleSongCoverOptions
import code.name.monkey.retromusic.model.Song
import com.bumptech.glide.Glide

/**
 * Adaptador de tarjetas horizontales para el carrusel de Top Charts en Inicio de Millay.
 */
class MillaySongCardAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<MillaySongCardAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverArt: ImageView = view.findViewById(R.id.coverArt)
        val title: TextView     = view.findViewById(R.id.songTitle)
        val artist: TextView    = view.findViewById(R.id.artistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_millay_song_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text  = song.title
        holder.artist.text = song.artistName

        Glide.with(holder.itemView.context)
            .load(RetroGlideExtension.getSongModel(song))
            .simpleSongCoverOptions(song)
            .into(holder.coverArt)

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount(): Int = songs.size
}
