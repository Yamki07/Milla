/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.deezer

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View

import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.song.SongAdapter
import code.name.monkey.retromusic.automix.AutomixPlayerEngine
import code.name.monkey.retromusic.automix.DeezerApiClient
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.automix.TidalDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



/**
 * Módulo UI "Milla Internet" para Búsqueda y Streaming en Alta Calidad desde Deezer.
 * Reutiliza el diseño nativo de tarjetas y paleta de RetroMusic [SongAdapter], al tiempo que interconecta
 * las pistas seleccionadas directamente con [AutomixPlayerEngine] y [DeezerDataSource].
 */
class DeezerFragment : AbsMainActivityFragment(R.layout.fragment_deezer_milla) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var toolbar: Toolbar

    private lateinit var adapter: DeezerSongAdapter
    private val songList = mutableListOf<Song>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        recyclerView = view.findViewById(R.id.recyclerView)

        setupToolbar()
        setupRecyclerView()
        setupSearchListeners()

        // Inicializar sesión ARL en segundo plano y cargar resultados populares por defecto
        DeezerApiClient.initSession(
            onSuccess = {
                performSearch("Salsa")
            },
            onError = {
                performSearch("Salsa")
            }
        )

        CoroutineScope(Dispatchers.Main).launch {
            TidalDownloadManager.downloadState.collect { state ->
                when (state) {
                    is TidalDownloadManager.DownloadState.Completed -> {
                        Toast.makeText(
                            requireContext(),
                            "Descarga completada en Milla DJ: ${state.song.title} 🎵",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is TidalDownloadManager.DownloadState.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Error en descarga: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }


    private fun setupToolbar() {
        toolbar.title = "Milla Internet (Deezer)"
        mainActivity.setSupportActionBar(toolbar)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Módulo listo para integrar menús contextuales en la barra superior si es necesario
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }


    private fun setupRecyclerView() {
        adapter = DeezerSongAdapter(
            requireActivity(),
            songList,
            R.layout.item_list
        ) { song, dataSet, position ->
            onDeezerSongClicked(song, dataSet, position)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearchListeners() {
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        DeezerApiClient.searchTracks(
            query = query,
            onResult = { songs ->
                activity?.runOnUiThread {
                    adapter.swapDataSet(songs)
                    adapter.notifyDataSetChanged()
                }
            },
            onError = { e ->
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error conectando con Deezer: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun onDeezerSongClicked(song: Song, dataSet: List<Song>, position: Int) {
        val currentEntity = song.toSongEntity()
        val nextEntity = if (position + 1 < dataSet.size) {
            dataSet[position + 1].toSongEntity()
        } else {
            null
        }

        CoroutineScope(Dispatchers.Main).launch {
            val enrichedCurrent = withContext(Dispatchers.IO) {
                BpmScanner.scanSongEntity(currentEntity)
            }
            val enrichedNext = if (nextEntity != null) {
                withContext(Dispatchers.IO) {
                    BpmScanner.scanSongEntity(nextEntity)
                }
            } else null

            val engine = AutomixPlayerEngine.getInstance(requireContext())
            engine.loadAndPlay(enrichedCurrent, enrichedNext)

            Toast.makeText(
                requireContext(),
                "Reproduciendo en Automix: ${song.title} (${if (enrichedCurrent.bpm > 0f) "${enrichedCurrent.bpm.toInt()} BPM" else "IA"})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun Song.toSongEntity(): SongEntity {
        return SongEntity(
            playlistCreatorId = 0L,
            id = this.id,
            title = this.title,
            trackNumber = this.trackNumber,
            year = this.year,
            duration = this.duration,
            data = "deezer://track/${this.id}",
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

    /**
     * Adaptador interno que hereda de [SongAdapter] para reutilizar diseño, colores de paleta y portadas
     * de RetroMusic, reemplazando el evento de clic con nuestro engine de Automix.
     */
    private class DeezerSongAdapter(
        activity: FragmentActivity,
        dataSet: MutableList<Song>,
        itemLayoutRes: Int,
        private val onSongClick: (Song, List<Song>, Int) -> Unit
    ) : SongAdapter(activity, dataSet, itemLayoutRes, true) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
            return DeezerViewHolder(view)
        }

        inner class DeezerViewHolder(itemView: View) : ViewHolder(itemView) {
            init {
                menu?.visibility = View.VISIBLE
                menu?.setImageResource(android.R.drawable.stat_sys_download)
                menu?.setOnClickListener {
                    if (layoutPosition in dataSet.indices) {
                        val song = dataSet[layoutPosition]
                        TidalDownloadManager.downloadTrack(activity, song)
                        Toast.makeText(
                            activity,
                            "Descarga iniciada: ${song.title} ⬇️",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }


            override fun onClick(v: View?) {
                if (isInQuickSelectMode) {
                    toggleChecked(layoutPosition)
                } else {
                    if (layoutPosition in dataSet.indices) {
                        onSongClick(dataSet[layoutPosition], dataSet, layoutPosition)
                    }
                }
            }
        }
    }
}

