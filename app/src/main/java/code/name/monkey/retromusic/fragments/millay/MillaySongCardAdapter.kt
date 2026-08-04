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
import com.bumptech.glide.Glide

data class DiscoverMix(
    val tag: String,
    val title: String,
    val coverUrl: String
)

/**
 * Adaptador de tarjetas para secciones Discover y Top Charts de ReFreezer.
 */
class MillaySongCardAdapter(
    private val items: List<DiscoverMix>,
    private val onItemClick: (DiscoverMix) -> Unit
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
        val mix = items[position]
        holder.title.text  = mix.tag.uppercase()
        holder.artist.text = mix.title

        Glide.with(holder.itemView.context)
            .load(mix.coverUrl)
            .placeholder(R.drawable.ic_album)
            .into(holder.coverArt)

        holder.itemView.setOnClickListener { onItemClick(mix) }
    }

    override fun getItemCount(): Int = items.size
}
