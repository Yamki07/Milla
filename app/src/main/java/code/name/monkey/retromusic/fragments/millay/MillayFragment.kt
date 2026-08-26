/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Streaming & Download Engine
 * Inspired by ReFreezer (DJDoubleD) — Dart → Kotlin port
 */
package code.name.monkey.retromusic.fragments.millay

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.TidalHifiApiClient
import code.name.monkey.retromusic.automix.TidalDownloadManager
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.fragments.millay.MillaySongRowAdapter
import code.name.monkey.retromusic.automix.MillayAlbumAdapter
import code.name.monkey.retromusic.automix.MillayGenreAdapter
import code.name.monkey.retromusic.db.toSong
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.fragments.settings.MillaySettingsFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.network.SupabaseClientManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

/**
 * Millay Fragment — Native Android UI
 *
 * Provides:
 *  1. Search → Deezer public API
 *  2. Streaming via download-decrypt-play pipeline
 *  3. Download with quality selection dialog
 *  4. Home screen with charts
 */
class MillayFragment : AbsMainActivityFragment(R.layout.fragment_millay) {

    // ─────────── Views ───────────
    private lateinit var searchInput: EditText
    private lateinit var loadingContainer: FrameLayout
    private lateinit var errorState: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var resultsHeader: LinearLayout
    private lateinit var resultsCount: TextView
    private lateinit var tracksList: RecyclerView

    // Home Views
    private lateinit var homeContent: LinearLayout
    private lateinit var topAlbumsList: RecyclerView
    private lateinit var topTracksList: RecyclerView
    private lateinit var genresList: RecyclerView
    private lateinit var miniPlayer: androidx.cardview.widget.CardView
    private lateinit var miniArt: ImageView
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniPlayPause: ImageButton
    private lateinit var miniNext: ImageButton
    private lateinit var miniDownload: ImageButton

    // ─────────── State ───────────
    private lateinit var trackAdapter: MillaySongRowAdapter
    private lateinit var topTracksAdapter: MillaySongRowAdapter
    private lateinit var topAlbumsAdapter: MillayAlbumAdapter
    private lateinit var genresAdapter: MillayGenreAdapter

    private var searchJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Song? = null
    private var isPlaying = false
    private val httpClient = OkHttpClient()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerView()
        setupSearch()
        setupMiniPlayerControls()
        loadHomeData()
    }

    // ─────────── View Binding ───────────
    private fun bindViews(view: View) {
        searchInput = view.findViewById(R.id.millaySearchInput)
        loadingContainer = view.findViewById(R.id.millayLoadingContainer)
        errorState = view.findViewById(R.id.millayErrorState)
        errorMessage = view.findViewById(R.id.millayErrorMessage)
        resultsHeader = view.findViewById(R.id.millayResultsHeader)
        resultsCount = view.findViewById(R.id.millayResultsCount)
        tracksList = view.findViewById(R.id.millayTracksList)

        homeContent = view.findViewById(R.id.millayHomeContent)
        topAlbumsList = view.findViewById(R.id.millayTopAlbumsList)
        topTracksList = view.findViewById(R.id.millayTopTracksList)
        genresList = view.findViewById(R.id.millayGenresList)
        miniPlayer = view.findViewById(R.id.millayMiniPlayer)
        miniArt = view.findViewById(R.id.millayMiniArt)
        miniTitle = view.findViewById(R.id.millayMiniTitle)
        miniArtist = view.findViewById(R.id.millayMiniArtist)
        miniPlayPause = view.findViewById(R.id.millayMiniPlayPause)
        miniNext = view.findViewById(R.id.millayMiniNext)
        miniDownload = view.findViewById(R.id.millayMiniDownload)

        view.findViewById<Button?>(R.id.millayRetryButton)?.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) performSearch(query)
        }
    }

    // ─────────── RecyclerView ───────────
    private fun setupRecyclerView() {
        trackAdapter = MillaySongRowAdapter(
            songs = emptyList(),
            onDownloadClick = { track -> showQualityDialog(track) },
            onSongClick = { track -> playTrack(track) }
        )
        tracksList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trackAdapter
            setHasFixedSize(false)
        }

        topTracksAdapter = MillaySongRowAdapter(
            songs = emptyList(),
            onDownloadClick = { track -> showQualityDialog(track) },
            onSongClick = { track -> playTrack(track) }
        )
        topTracksList.apply {
            adapter = topTracksAdapter
        }

        topAlbumsAdapter = MillayAlbumAdapter(emptyList()) { album ->
            performSearch(album["title"] ?: "")
        }
        topAlbumsList.apply {
            adapter = topAlbumsAdapter
        }

        val staticGenres = listOf(
            mapOf("name" to "Pop", "color" to "#148A08"),
            mapOf("name" to "Hip-Hop", "color" to "#E8115B"),
            mapOf("name" to "Latin", "color" to "#BA5D07"),
            mapOf("name" to "Rock", "color" to "#E1118C"),
            mapOf("name" to "R&B", "color" to "#8C1932"),
            mapOf("name" to "Indie", "color" to "#E91429"),
            mapOf("name" to "Dance", "color" to "#D84000"),
            mapOf("name" to "Country", "color" to "#F59B23")
        )
        genresAdapter = MillayGenreAdapter(staticGenres) { genre ->
            performSearch(genre["name"] ?: "")
        }
        genresList.apply {
            adapter = genresAdapter
        }
    }

    private fun loadHomeData() {
        lifecycleScope.launch {
            try {
                val tracks = TidalHifiApiClient.searchTracks("Top Hits")

                val albums = listOf(
                    mapOf("title" to "Populares", "cover" to ""),
                    mapOf("title" to "Global", "cover" to ""),
                    mapOf("title" to "Novedades", "cover" to "")
                )

                topAlbumsAdapter = MillayAlbumAdapter(albums) { album ->
                    performSearch(album["title"] ?: "")
                }
                topAlbumsList.adapter = topAlbumsAdapter

                topTracksAdapter = MillaySongRowAdapter(
                    songs = tracks,
                    onDownloadClick = { track -> showQualityDialog(track) },
                    onSongClick = { track -> playTrack(track) }
                )
                topTracksList.adapter = topTracksAdapter
            } catch (e: Exception) {
                // Ignore silently
            }
        }
    }

    // ─────────── Search ───────────
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: return
                searchJob?.cancel()
                if (query.length < 2) {
                    showState(State.HOME)
                    return
                }
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(600)
                    performSearch(query)
                }
            }
        })
    }

    private fun performSearch(query: String) {
        showState(State.LOADING)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tracks = TidalHifiApiClient.searchTracks(query)
                if (tracks.isEmpty()) {
                    showState(State.EMPTY)
                } else {
                    trackAdapter = MillaySongRowAdapter(
                        songs = tracks,
                        onDownloadClick = { track -> showQualityDialog(track) },
                        onSongClick = { track -> playTrack(track) }
                    )
                    tracksList.adapter = trackAdapter
                    resultsCount.text = "${tracks.size} canciones"
                    showState(State.RESULTS)
                }
            } catch (e: Exception) {
                errorMessage.text = "Error al buscar: ${e.message ?: "Verifica tu conexión."}"
                showState(State.ERROR)
            }
        }
    }

    // ─────────── UI State Machine ───────────
    private enum class State { LOADING, HOME, EMPTY, RESULTS, ERROR }

    private fun showState(state: State) {
        loadingContainer.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        homeContent.visibility = if (state == State.HOME) View.VISIBLE else View.GONE
        errorState.visibility = if (state == State.ERROR) View.VISIBLE else View.GONE
        tracksList.visibility = if (state == State.RESULTS) View.VISIBLE else View.GONE
        resultsHeader.visibility = if (state == State.RESULTS) View.VISIBLE else View.GONE
    }

    // ─────────── Playback (Streaming Nativo) ───────────
    private fun playTrack(track: Song) {
        val ctx = context ?: return
        try {
            Toast.makeText(ctx, "▶ Conectando stream: ${track.title}...", Toast.LENGTH_SHORT).show()

            val songEntity = code.name.monkey.retromusic.db.SongEntity(
                playlistCreatorId = 0L,
                id = track.id,
                title = track.title,
                trackNumber = track.trackNumber,
                year = track.year,
                duration = track.duration,
                data = track.data,
                dateModified = System.currentTimeMillis(),
                albumId = track.albumId,
                albumName = track.albumName,
                artistId = track.artistId,
                artistName = track.artistName,
                composer = track.composer,
                albumArtist = track.albumArtist,
                bpm = 120f
            )

            code.name.monkey.retromusic.helper.MusicPlayerRemote.openQueue(listOf(songEntity.toSong()), 0, true)
            
            currentTrack = track
            isPlaying = true
            showMiniPlayer(track)
            miniPlayPause.setImageResource(R.drawable.ic_pause_white_48dp)

        } catch (e: Exception) {
            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMiniPlayer(track: Song) {
        miniTitle.text = track.title
        miniArtist.text = track.artistName
        miniPlayPause.setImageResource(R.drawable.ic_pause_white_48dp)
        miniPlayer.visibility = View.VISIBLE

        val uri = code.name.monkey.retromusic.glide.RetroGlideExtension.getSongModel(track)
        Glide.with(miniArt.context)
            .load(uri)
            .apply(RequestOptions().transform(RoundedCorners(12)).placeholder(R.drawable.millay_art_placeholder))
            .into(miniArt)
    }

    // ─────────── Mini Player Controls ───────────
    private fun setupMiniPlayerControls() {
        miniPlayPause.setOnClickListener {
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
                miniPlayPause.setImageResource(R.drawable.ic_play_arrow_white_32dp)
            } else {
                MusicPlayerRemote.resumePlaying()
                miniPlayPause.setImageResource(R.drawable.ic_pause_white_48dp)
            }
        }

        miniNext.setOnClickListener {
            MusicPlayerRemote.playNextSong()
        }

        miniDownload.setOnClickListener {
            currentTrack?.let { showQualityDialog(it) }
        }

        miniPlayer.setOnClickListener {
            // Usually we can expand the sliding panel here if needed
            val activity = requireActivity() as? code.name.monkey.retromusic.activities.MainActivity
            activity?.expandPanel()
        }
    }

    // ─────────── Quality Selection Dialog ───────────
    private fun showQualityDialog(track: Song) {
        val ctx = context ?: return
        
        val labels = arrayOf("HI_RES_LOSSLESS (FLAC)", "HIGH (MP3 320kbps)", "LOW (MP3 96kbps)")
        
        AlertDialog.Builder(ctx)
            .setTitle("⬇️ Descargar: ${track.title}")
            .setItems(labels) { _, which ->
                val (quality, format) = when(which) {
                    0 -> Pair(9, "FLAC")
                    1 -> Pair(3, "MP3_320")
                    else -> Pair(1, "MP3_96")
                }
                startDownload(track, quality, format)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ─────────── Download ───────────
    private fun startDownload(track: Song, quality: Int, format: String) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(ctx, "⬇️ Descargando en $format: ${track.title}", Toast.LENGTH_SHORT).show()
                TidalDownloadManager.downloadTrack(ctx, track, quality)
            } catch (e: Exception) {
                val currentContext = context ?: return@launch
                Toast.makeText(currentContext, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────── Lifecycle ───────────
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
    override fun onMenuItemSelected(item: MenuItem): Boolean = false
}
