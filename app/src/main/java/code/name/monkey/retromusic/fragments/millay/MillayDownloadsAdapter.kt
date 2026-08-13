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
import code.name.monkey.retromusic.automix.TidalDownloadManager
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Modelo de un ítem de descarga en la pestaña Descargas de Millay.
 */
data class MillayDownloadItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val quality: String,
    val progress: Int,
    val status: TidalDownloadManager.Status
)

/**
 * Adaptador para la lista de descargas de Millay con barra de progreso en tiempo real.
 */
class MillayDownloadsAdapter(
    private val items: MutableList<MillayDownloadItem>
) : RecyclerView.Adapter<MillayDownloadsAdapter.DownloadViewHolder>() {

    inner class DownloadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusIcon: ImageView                = view.findViewById(R.id.statusIcon)
        val songTitle: TextView                  = view.findViewById(R.id.songTitle)
        val artistName: TextView                 = view.findViewById(R.id.artistName)
        val qualityBadge: TextView               = view.findViewById(R.id.qualityBadge)
        val downloadProgress: LinearProgressIndicator = view.findViewById(R.id.downloadProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_millay_download, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val item = items[position]
        holder.songTitle.text   = item.title.ifEmpty { "Pista ${item.trackId}" }
        holder.artistName.text  = item.artist.ifEmpty { "Deezer" }
        holder.qualityBadge.text = item.quality

        when (item.status) {
            TidalDownloadManager.Status.DOWNLOADING -> {
                holder.downloadProgress.isVisible = true
                holder.downloadProgress.progress  = item.progress
                holder.statusIcon.setImageResource(android.R.drawable.stat_sys_download)
            }
            TidalDownloadManager.Status.POST_PROCESSING -> {
                holder.downloadProgress.isVisible = true
                holder.downloadProgress.isIndeterminate = true
                holder.statusIcon.setImageResource(android.R.drawable.stat_notify_sync)
            }
            TidalDownloadManager.Status.DONE -> {
                holder.downloadProgress.isVisible = false
                holder.statusIcon.setImageResource(android.R.drawable.checkbox_on_background)
            }
            TidalDownloadManager.Status.ERROR -> {
                holder.downloadProgress.isVisible = false
                holder.statusIcon.setImageResource(android.R.drawable.stat_notify_error)
            }
            else -> {
                holder.downloadProgress.isVisible = false
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
