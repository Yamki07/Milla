/*
 * Copyright (c) 2026 Milla / Millay – Deezer Native Streaming & Download Engine
 * Inspired by ReFreezer (DJDoubleD) — Dart → Kotlin port
 */
package code.name.monkey.retromusic.fragments.millay

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
import code.name.monkey.retromusic.automix.DeezerApiClient
import code.name.monkey.retromusic.automix.DeezerDownloadManager
import code.name.monkey.retromusic.automix.DeezerTrack
import code.name.monkey.retromusic.automix.MillayTrackAdapter
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import code.name.monkey.retromusic.automix.MillayAlbumAdapter
import code.name.monkey.retromusic.automix.MillayGenreAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Millay Fragment — Native Android UI
 *
 * Provides:
 *  1. Search bar → queries Deezer's private GW-light API (ported from ReFreezer)
 *  2. Track list with glassmorphism cards (quality badges, play & download buttons)
 *  3. Streaming via MediaPlayer with a floating mini-player bar
 *  4. Background downloading via DeezerDownloadManager
 *
 * Design: Monochrome dark palette (#080A12) + Apple Music glassmorphism effects
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
    private lateinit var trackAdapter: MillayTrackAdapter
    private lateinit var topTracksAdapter: MillayTrackAdapter
    private lateinit var topAlbumsAdapter: MillayAlbumAdapter
    private lateinit var genresAdapter: MillayGenreAdapter
    
    private var searchJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: DeezerTrack? = null
    private var isPlaying = false

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
        trackAdapter = MillayTrackAdapter(
            onPlay = { track -> playTrack(track) },
            onDownload = { track -> downloadTrack(track) }
        )
        tracksList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trackAdapter
            setHasFixedSize(false)
        }

        // Top Tracks (Home)
        topTracksAdapter = MillayTrackAdapter(
            onPlay = { track -> playTrack(track) },
            onDownload = { track -> downloadTrack(track) }
        )
        topTracksList.apply {
            adapter = topTracksAdapter
        }

        // Top Albums (Home)
        topAlbumsAdapter = MillayAlbumAdapter(emptyList()) { album ->
            performSearch(album["title"] ?: "")
        }
        topAlbumsList.apply {
            adapter = topAlbumsAdapter
        }

        // Genres (Home)
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
                val albums = DeezerApiClient.getTopAlbums()
                val tracks = DeezerApiClient.getTopTracks()
                
                topAlbumsAdapter = MillayAlbumAdapter(albums) { album ->
                    performSearch(album["title"] ?: "")
                }
                topAlbumsList.adapter = topAlbumsAdapter
                
                topTracksAdapter.submitList(tracks)
            } catch (e: Exception) {
                // Ignore home load errors silently
            }
        }
    }

    // ─────────── Search with debounce ───────────
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
                // Debounce: wait 600ms after last keystroke before firing request
                searchJob = lifecycleScope.launch {
                    delay(600)
                    performSearch(query)
                }
            }
        })
    }

    // ─────────── Search Execution ───────────
    private fun performSearch(query: String) {
        showState(State.LOADING)
        lifecycleScope.launch {
            try {
                val tracks = DeezerApiClient.search(query)
                if (tracks.isEmpty()) {
                    showState(State.EMPTY)
                } else {
                    trackAdapter.submitList(tracks)
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

    // ─────────── Playback ───────────
    private fun playTrack(track: DeezerTrack) {
        lifecycleScope.launch {
            try {
                // Get the CDN stream URL from Deezer (uses MD5 + Blowfish decryption)
                val streamUrl = DeezerApiClient.getStreamUrl(track)
                if (streamUrl.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "No se pudo obtener el stream. Verifica tu ARL.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Stop previous playback
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                currentTrack = track
                isPlaying = true

                // Show mini player
                showMiniPlayer(track)

                // Start MediaPlayer with Deezer's encrypted stream
                // (DeezerDataSource handles real-time Blowfish decryption)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(streamUrl)
                    prepareAsync()
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        this@MillayFragment.isPlaying = false
                        miniPlayPause.setImageResource(R.drawable.ic_play_arrow_white_32dp)
                    }
                    setOnErrorListener { _, what, extra ->
                        Toast.makeText(requireContext(), "Error de reproducción ($what/$extra)", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMiniPlayer(track: DeezerTrack) {
        miniTitle.text = track.title
        miniArtist.text = track.artistName
        miniPlayPause.setImageResource(R.drawable.ic_pause_white_48dp)
        miniPlayer.visibility = View.VISIBLE

        Glide.with(miniArt.context)
            .load(track.coverUrlThumb)
            .apply(RequestOptions().transform(RoundedCorners(12)).placeholder(R.drawable.millay_art_placeholder))
            .into(miniArt)
    }

    // ─────────── Mini Player Controls ───────────
    private fun setupMiniPlayerControls() {
        miniPlayPause.setOnClickListener {
            val mp = mediaPlayer ?: return@setOnClickListener
            if (isPlaying) {
                mp.pause()
                isPlaying = false
                miniPlayPause.setImageResource(R.drawable.ic_play_arrow_white_32dp)
            } else {
                mp.start()
                isPlaying = true
                miniPlayPause.setImageResource(R.drawable.ic_pause_white_48dp)
            }
        }

        miniDownload.setOnClickListener {
            currentTrack?.let { downloadTrack(it) }
        }
    }

    // ─────────── Download ───────────
    private fun downloadTrack(track: DeezerTrack) {
        lifecycleScope.launch {
            try {
                Toast.makeText(requireContext(), "⬇️ Descargando: ${track.title}", Toast.LENGTH_SHORT).show()
                // Convert DeezerTrack to Song for DeezerDownloadManager
                val song = code.name.monkey.retromusic.model.Song(
                    id = track.id.toLongOrNull() ?: 0L,
                    title = track.title,
                    trackNumber = 1,
                    year = 2026,
                    duration = track.durationSec * 1000L,
                    data = "deezer://track/${track.id}",
                    dateModified = System.currentTimeMillis(),
                    albumId = 0L,
                    albumName = track.albumTitle,
                    artistId = 0L,
                    artistName = track.artistName,
                    composer = "",
                    albumArtist = track.artistName
                )
                DeezerDownloadManager.downloadTrack(requireContext(), song, quality = 9)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────── Lifecycle ───────────
    override fun onPause() {
        super.onPause()
        // Keep playing in background (music app behavior)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        // Don't release MediaPlayer here — keep background playback alive
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
    override fun onMenuItemSelected(item: MenuItem): Boolean = false
}
