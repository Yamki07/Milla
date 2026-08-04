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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.simpleSongCoverOptions
import code.name.monkey.retromusic.model.Song
import com.bumptech.glide.Glide

/**
 * Adaptador de lista vertical de canciones para la Búsqueda y Recomendados de Millay.
 * Incluye botón de descarga individual y soporta un listener separado para descarga.
 *
 * @param onDownloadClick Listener para el botón de descarga (opcional, mostrado en tab Buscar).
 * @param onSongClick     Listener para clic en la canción (reproducción con Automix).
 */
class MillaySongRowAdapter(
    private val songs: List<Song>,
    private val onDownloadClick: ((Song) -> Unit)? = null,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<MillaySongRowAdapter.RowViewHolder>() {

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverArt: ImageView       = view.findViewById(R.id.image)
        val title: TextView           = view.findViewById(R.id.title)
        val text: TextView            = view.findViewById(R.id.text)
        val menu: ImageView           = view.findViewById(R.id.menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.text.text  = song.artistName

        Glide.with(holder.itemView.context)
            .load(RetroGlideExtension.getSongModel(song))
            .simpleSongCoverOptions(song)
            .into(holder.coverArt)

        // Botón de descarga (visible solo si hay listener configurado)
        if (onDownloadClick != null) {
            holder.menu.isVisible = true
            holder.menu.setImageResource(android.R.drawable.stat_sys_download)
            holder.menu.setOnClickListener { onDownloadClick.invoke(song) }
        } else {
            holder.menu.isVisible = false
        }

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount(): Int = songs.size
}
