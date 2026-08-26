/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.automix.TidalHifiApiClient
import code.name.monkey.retromusic.automix.TidalDownloadManager
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Tab 2 de Millay — Búsqueda en Alta Calidad con filtros de formato (FLAC / MP3 320).
 * Conectada con el PlaybackOrchestrator mediante MusicPlayerRemote y TidalDownloadManager.
 */
class MillaySearchFragment : Fragment(R.layout.fragment_millay_search) {

    private lateinit var searchEditText: EditText
    private lateinit var qualityChipGroup: ChipGroup
    private lateinit var searchResultsRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout

    // Calidad seleccionada: 9 = FLAC, 3 = MP3 320
    private var selectedQuality = 9

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchEditText       = view.findViewById(R.id.searchEditText)
        qualityChipGroup     = view.findViewById(R.id.qualityChipGroup)
        searchResultsRecycler= view.findViewById(R.id.searchResultsRecycler)
        emptyState           = view.findViewById(R.id.emptyState)

        setupRecyclerView()
        setupSearchListeners()
        setupQualityChips()
    }

    private fun setupRecyclerView() {
        searchResultsRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupQualityChips() {
        qualityChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedQuality = when (checkedIds.firstOrNull()) {
                R.id.chipFlac -> 9
                R.id.chipMp3  -> 3
                else          -> 9
            }
        }
    }

    private fun setupSearchListeners() {
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isEmpty()) return

        emptyState.isVisible = false
        searchResultsRecycler.isVisible = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tidalDeferred = async { TidalHifiApiClient.searchTracks(query) }
                val mergedSongs = try { tidalDeferred.await() } catch(e: Exception) { emptyList() }

                withContext(Dispatchers.Main) {
                    if (mergedSongs.isEmpty()) {
                        emptyState.isVisible = true
                    } else {
                        searchResultsRecycler.isVisible = true
                        searchResultsRecycler.adapter = MillaySongRowAdapter(
                            songs = mergedSongs,
                            onDownloadClick = { song ->
                                TidalDownloadManager.downloadTrack(requireContext(), song)
                                
                                Toast.makeText(
                                    requireContext(),
                                    "⬇️ Descargando: ${song.title} (${song.composer})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) { song ->
                            onSongClicked(song, mergedSongs)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    emptyState.isVisible = true
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onSongClicked(song: Song, dataSet: List<Song>) {
        val position = dataSet.indexOf(song)
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) { BpmScanner.scanSongEntity(song.toSongEntity()) }
            MusicPlayerRemote.startInfiniteRadio(song, dataSet.drop(position))
            Toast.makeText(
                requireContext(),
                "▶️ ${song.title} · ${song.artistName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun Song.toSongEntity(): SongEntity = SongEntity(
        playlistCreatorId = 0L,
        id = this.id,
        title = this.title,
        trackNumber = this.trackNumber,
        year = this.year,
        duration = this.duration,
        data = this.data,
        dateModified = this.dateModified,
        albumId = this.albumId,
        albumName = this.albumName,
        artistId = this.artistId,
        artistName = this.artistName,
        composer = this.composer,
        albumArtist = this.albumArtist,
        bpm = 0f,
        replayGain = 0f,
        musicalKey = "",
        cueOutMs = 0L
    )
}
