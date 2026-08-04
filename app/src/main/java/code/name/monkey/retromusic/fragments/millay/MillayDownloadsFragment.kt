/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.DeezerDownloadManager
import kotlinx.coroutines.launch

/**
 * Tab 3 de Millay — Lista de descargas activas y completadas desde Deezer.
 * Observa [DeezerDownloadManager.downloadState] con StateFlow para actualizarse en tiempo real.
 */
class MillayDownloadsFragment : Fragment(R.layout.fragment_millay_downloads) {

    private lateinit var downloadsRecycler: RecyclerView
    private lateinit var emptyDownloads: LinearLayout

    // Lista mutable de ítems de descarga para el adapter
    private val downloadItems = mutableListOf<MillayDownloadItem>()
    private lateinit var downloadAdapter: MillayDownloadsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadsRecycler = view.findViewById(R.id.downloadsRecycler)
        emptyDownloads    = view.findViewById(R.id.emptyDownloads)

        setupRecyclerView()
        observeDownloads()
    }

    private fun setupRecyclerView() {
        downloadAdapter = MillayDownloadsAdapter(downloadItems)
        downloadsRecycler.layoutManager = LinearLayoutManager(requireContext())
        downloadsRecycler.adapter = downloadAdapter
    }

    /**
     * Escucha el StateFlow de DeezerDownloadManager y actualiza la lista en tiempo real.
     */
    private fun observeDownloads() {
        viewLifecycleOwner.lifecycleScope.launch {
            DeezerDownloadManager.downloadState.collect { state ->
                when (state) {
                    is DeezerDownloadManager.DownloadState.Downloading -> {
                        val existing = downloadItems.indexOfFirst { it.trackId == state.trackId }
                        if (existing >= 0) {
                            downloadItems[existing] = downloadItems[existing].copy(
                                progress = state.progress,
                                status = DeezerDownloadManager.Status.DOWNLOADING
                            )
                            downloadAdapter.notifyItemChanged(existing)
                        } else {
                            downloadItems.add(0, MillayDownloadItem(
                                trackId = state.trackId,
                                title = state.trackId,
                                artist = "",
                                quality = "FLAC",
                                progress = state.progress,
                                status = DeezerDownloadManager.Status.DOWNLOADING
                            ))
                            downloadAdapter.notifyItemInserted(0)
                        }
                        updateEmptyState()
                    }
                    is DeezerDownloadManager.DownloadState.Completed -> {
                        val existing = downloadItems.indexOfFirst { it.trackId == state.trackId }
                        val completed = MillayDownloadItem(
                            trackId = state.trackId,
                            title = state.song.title,
                            artist = state.song.artistName,
                            quality = if (state.filePath.endsWith(".flac")) "FLAC" else "MP3 320",
                            progress = 100,
                            status = DeezerDownloadManager.Status.DONE
                        )
                        if (existing >= 0) {
                            downloadItems[existing] = completed
                            downloadAdapter.notifyItemChanged(existing)
                        } else {
                            downloadItems.add(0, completed)
                            downloadAdapter.notifyItemInserted(0)
                        }
                        updateEmptyState()
                    }
                    is DeezerDownloadManager.DownloadState.Error -> {
                        val existing = downloadItems.indexOfFirst { it.trackId == state.trackId }
                        if (existing >= 0) {
                            downloadItems[existing] = downloadItems[existing].copy(
                                status = DeezerDownloadManager.Status.ERROR
                            )
                            downloadAdapter.notifyItemChanged(existing)
                        }
                    }
                    else -> { /* Idle — sin cambios */ }
                }
            }
        }
    }

    private fun updateEmptyState() {
        emptyDownloads.isVisible = downloadItems.isEmpty()
        downloadsRecycler.isVisible = downloadItems.isNotEmpty()
    }
}
