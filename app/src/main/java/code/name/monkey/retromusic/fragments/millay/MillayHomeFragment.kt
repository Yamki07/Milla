/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.AutomixPlayerEngine
import code.name.monkey.retromusic.automix.AutomixRadioEngine
import code.name.monkey.retromusic.automix.BpmScanner
import code.name.monkey.retromusic.automix.DeezerApiClient
import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab 1 de Millay — Pantalla de Inicio con Flow Bubbles (Mood/Radio),
 * Top Charts horizontal y lista de Recomendados.
 */
class MillayHomeFragment : Fragment(R.layout.fragment_millay_home) {

    private lateinit var flowBubblesRecycler: RecyclerView
    private lateinit var topChartsRecycler: RecyclerView
    private lateinit var recommendedRecycler: RecyclerView

    // Burbujas de Flow basadas en géneros/moods populares
    private val flowBubbles = listOf(
        FlowBubble("🕺", "Salsa", "Clásica"),
        FlowBubble("💃", "Bachata", "Romántica"),
        FlowBubble("🎵", "Merengue", "Festivo"),
        FlowBubble("🎸", "Rock", "Energético"),
        FlowBubble("🎤", "Pop", "Hits"),
        FlowBubble("🎧", "Reggaetón", "Urban"),
        FlowBubble("🎷", "Jazz", "Clásico"),
        FlowBubble("🔮", "DJ Set", "Infinito"),
        FlowBubble("😊", "Alegre", "Mood"),
        FlowBubble("🌙", "Chill", "Relax"),
        FlowBubble("💪", "Ejercicio", "Energy"),
        FlowBubble("❤️", "Amor", "Baladas"),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowBubblesRecycler  = view.findViewById(R.id.flowBubblesRecyclerView)
        topChartsRecycler    = view.findViewById(R.id.topChartsRecycler)
        recommendedRecycler  = view.findViewById(R.id.recommendedRecycler)

        setupFlowBubbles()
        loadTopCharts()
        loadRecommended()
    }

    // ------------------------------------------------------------------
    // Flow Bubbles — Radio de Mood / Género
    // ------------------------------------------------------------------
    private fun setupFlowBubbles() {
        val adapter = FlowBubbleAdapter(flowBubbles) { bubble ->
            onFlowBubbleClicked(bubble)
        }
        flowBubblesRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        flowBubblesRecycler.adapter = adapter
    }

    private fun onFlowBubbleClicked(bubble: FlowBubble) {
        val query = bubble.title
        Toast.makeText(requireContext(), "🔮 Flow: ${bubble.title} iniciado...", Toast.LENGTH_SHORT).show()
        DeezerApiClient.searchTracks(
            query = query,
            onResult = { songs ->
                if (songs.isNotEmpty()) {
                    launchRadioWithSongs(songs)
                }
            }
        )
    }

    private fun launchRadioWithSongs(songs: List<Song>) {
        CoroutineScope(Dispatchers.Main).launch {
            val entities = songs.map { it.toSongEntity() }
            val enriched = withContext(Dispatchers.IO) {
                entities.map { BpmScanner.scanSongEntity(it) }
            }
            val engine = AutomixPlayerEngine.getInstance(requireContext())
            if (enriched.isNotEmpty()) {
                engine.loadAndPlay(enriched[0], enriched.getOrNull(1))
                // Lanzar AutomixRadioEngine para continuar la radio infinita
                AutomixRadioEngine.getInstance(requireContext())
                    .startUniversalDjSet(enriched[0], enriched)
            }
        }
    }

    // ------------------------------------------------------------------
    // Top Charts — Carga desde Deezer API
    // ------------------------------------------------------------------
    private fun loadTopCharts() {
        topChartsRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        DeezerApiClient.searchTracks(
            query = "top hits 2026",
            onResult = { songs ->
                activity?.runOnUiThread {
                    topChartsRecycler.adapter = MillaySongCardAdapter(songs) { song ->
                        onSongClicked(song, songs)
                    }
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // Recomendados — Carga desde Deezer API
    // ------------------------------------------------------------------
    private fun loadRecommended() {
        recommendedRecycler.layoutManager = LinearLayoutManager(requireContext())

        DeezerApiClient.searchTracks(
            query = "salsa merengue 2026",
            onResult = { songs ->
                activity?.runOnUiThread {
                    recommendedRecycler.adapter = MillaySongRowAdapter(songs) { song ->
                        onSongClicked(song, songs)
                    }
                }
            }
        )
    }

    // ------------------------------------------------------------------
    // Reproducir con Automix
    // ------------------------------------------------------------------
    private fun onSongClicked(song: Song, dataSet: List<Song>) {
        val position = dataSet.indexOf(song)
        CoroutineScope(Dispatchers.Main).launch {
            val current = withContext(Dispatchers.IO) { BpmScanner.scanSongEntity(song.toSongEntity()) }
            val next = if (position + 1 < dataSet.size) {
                withContext(Dispatchers.IO) { BpmScanner.scanSongEntity(dataSet[position + 1].toSongEntity()) }
            } else null

            AutomixPlayerEngine.getInstance(requireContext()).loadAndPlay(current, next)
            Toast.makeText(
                requireContext(),
                "▶️ ${song.title} (${if (current.bpm > 0f) "${current.bpm.toInt()} BPM" else "IA"})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ------------------------------------------------------------------
    // Extensión de conversión Song → SongEntity
    // ------------------------------------------------------------------
    private fun Song.toSongEntity(): SongEntity = SongEntity(
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

    // ------------------------------------------------------------------
    // Modelo de datos para las burbujas de Flow
    // ------------------------------------------------------------------
    data class FlowBubble(val emoji: String, val title: String, val subtitle: String)
}
