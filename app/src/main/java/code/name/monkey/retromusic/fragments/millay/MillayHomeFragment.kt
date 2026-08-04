/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.AutomixPlayerEngine
import code.name.monkey.retromusic.automix.DeezerApiClient
import code.name.monkey.retromusic.automix.toSongEntity
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

/**
 * Pantalla principal de Inicio Millay (Réplica 1:1 ReFreezer).
 */
class MillayHomeFragment : Fragment() {

    private lateinit var flowRecycler: RecyclerView
    private lateinit var continueStreamingRecycler: RecyclerView
    private lateinit var discoverRecycler: RecyclerView
    private lateinit var topChartsRecycler: RecyclerView

    private lateinit var btnHomeDownloads: ShapeableImageView
    private lateinit var btnHomeSettings: ShapeableImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_millay_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowRecycler = view.findViewById(R.id.flowRecycler)
        continueStreamingRecycler = view.findViewById(R.id.continueStreamingRecycler)
        discoverRecycler = view.findViewById(R.id.discoverRecycler)
        topChartsRecycler = view.findViewById(R.id.topChartsRecycler)

        btnHomeDownloads = view.findViewById(R.id.btnHomeDownloads)
        btnHomeSettings = view.findViewById(R.id.btnHomeSettings)

        setupButtons()
        setupFlowBubbles()
        setupContinueStreaming()
        setupDiscover()
        setupTopCharts()
    }

    private fun setupButtons() {
        btnHomeDownloads.setOnClickListener {
            Toast.makeText(context, "Gestor de Descargas ReFreezer", Toast.LENGTH_SHORT).show()
        }

        btnHomeSettings.setOnClickListener {
            Toast.makeText(context, "Configuración ReFreezer / Milla", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFlowBubbles() {
        flowRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val bubbles = listOf(
            FlowBubble("Flow", "🌊 Universal Mix", "#FF007A", "🎵"),
            FlowBubble("Workout", "👟 High Energy", "#FF5722", "👟"),
            FlowBubble("Chill", "🛋️ Relaxation", "#00BCD4", "🛋️"),
            FlowBubble("Party", "🎉 Dance Hits", "#9C27B0", "🎉"),
            FlowBubble("Focus", "🧠 Deep Tech", "#3F51B5", "🧠"),
            FlowBubble("Sad", "🌧️ Melancholy", "#607D8B", "🌧️")
        )
        flowRecycler.adapter = FlowBubbleAdapter(bubbles) { bubble ->
            playFlowMix(bubble.name)
        }
    }

    private fun setupContinueStreaming() {
        continueStreamingRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val artists = listOf(
            ArtistItem("David Guetta", "https://e-cdns-images.dzcdn.net/images/artist/f29a0b06920839e94c2db6e001944890/500x500.jpg"),
            ArtistItem("Wildstylez", "https://e-cdns-images.dzcdn.net/images/artist/b1784c04ce834dd9b8b09062eeebaaeb/500x500.jpg"),
            ArtistItem("Martin Garrix", "https://e-cdns-images.dzcdn.net/images/artist/3331cfad93dd3901b0f58980bfaaa45b/500x500.jpg"),
            ArtistItem("Tiësto", "https://e-cdns-images.dzcdn.net/images/artist/a8947b01d368eb1e27a6f20b8f106f23/500x500.jpg"),
            ArtistItem("Daft Punk", "https://e-cdns-images.dzcdn.net/images/artist/f7e02b7405105eb0ef9a3c80bf61b9b1/500x500.jpg")
        )
        continueStreamingRecycler.adapter = ContinueStreamingAdapter(artists) { artist ->
            searchAndPlay(artist.name)
        }
    }

    private fun setupDiscover() {
        discoverRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val mixes = listOf(
            DiscoverMix("daily", "Featuring Tiësto, Dimitri Vegas & Like Mike...", "https://e-cdns-images.dzcdn.net/images/cover/9812dd6ff8b813ed946e3ee9e5590924/500x500.jpg"),
            DiscoverMix("daily", "Featuring Psyko Punkz, D-block & S-te-fan...", "https://e-cdns-images.dzcdn.net/images/cover/77b8b4033ce2a10bebb3288ec7b09bf9/500x500.jpg"),
            DiscoverMix("daily", "Featuring Exodus, Angels...", "https://e-cdns-images.dzcdn.net/images/cover/11833d7b87c7161e1fa4e0e5a639b7ef/500x500.jpg")
        )
        discoverRecycler.adapter = MillaySongCardAdapter(mixes) { mix ->
            searchAndPlay(mix.title)
        }
    }

    private fun setupTopCharts() {
        topChartsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        DeezerApiClient.searchTracks("Top 50 Global", onResult = { songs ->
            if (songs.isNotEmpty()) {
                activity?.runOnUiThread {
                    val mixes = songs.take(10).map { song ->
                        DiscoverMix("TOP", "${song.title} - ${song.artistName}", "https://e-cdns-images.dzcdn.net/images/cover/${song.albumId}/500x500.jpg")
                    }
                    topChartsRecycler.adapter = MillaySongCardAdapter(mixes) { mix ->
                        searchAndPlay(mix.title)
                    }
                }
            }
        })
    }

    private fun playFlowMix(mood: String) {
        Toast.makeText(context, "Iniciando Flow ReFreezer ($mood)...", Toast.LENGTH_SHORT).show()
        DeezerApiClient.searchTracks(mood, onResult = { songs ->
            if (songs.isNotEmpty()) {
                val songEntity = songs.first().toSongEntity()
                activity?.runOnUiThread {
                    AutomixPlayerEngine.getInstance(requireContext()).loadAndPlay(songEntity)
                }
            }
        })
    }

    private fun searchAndPlay(query: String) {
        DeezerApiClient.searchTracks(query, onResult = { songs ->
            if (songs.isNotEmpty()) {
                val songEntity = songs.first().toSongEntity()
                activity?.runOnUiThread {
                    AutomixPlayerEngine.getInstance(requireContext()).loadAndPlay(songEntity)
                }
            }
        })
    }

    // ---------------------------------------------------------------------------
    // Adaptadores ReFreezer
    // ---------------------------------------------------------------------------
    data class ArtistItem(val name: String, val imageUrl: String)

    private inner class ContinueStreamingAdapter(
        private val list: List<ArtistItem>,
        private val onClick: (ArtistItem) -> Unit
    ) : RecyclerView.Adapter<ContinueStreamingAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val image: ShapeableImageView = v.findViewById(R.id.artistImage)
            val name: TextView = v.findViewById(R.id.artistName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_millay_continue_streaming, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            Glide.with(holder.itemView)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_artist)
                .into(holder.image)

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = list.size
    }
}
