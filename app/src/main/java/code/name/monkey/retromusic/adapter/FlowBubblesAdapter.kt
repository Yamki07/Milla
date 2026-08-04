/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.automix.AutomixRadioEngine
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.SongRepository
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

data class FlowBubbleItem(
    val id: Int,
    val emoji: String,
    val title: String,
    val subtitle: String
)

/**
 * Adaptador para las 5 burbujas horizontales circulares en Inicio ("Flow: Dale play a lo que sientes")
 * estilizadas con MaterialYou del tema nativo de RetroMusic.
 */
class FlowBubblesAdapter(
    private val items: List<FlowBubbleItem>,
    private val onItemClick: (FlowBubbleItem) -> Unit
) : RecyclerView.Adapter<FlowBubblesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: MaterialTextView = itemView.findViewById(R.id.bubbleEmoji)
        val titleText: MaterialTextView = itemView.findViewById(R.id.bubbleTitle)
        val subtitleText: MaterialTextView = itemView.findViewById(R.id.bubbleSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flow_bubble_milla, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.emojiText.text = item.emoji
        holder.titleText.text = item.title
        holder.subtitleText.text = item.subtitle
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        fun getDefaultItems(): List<FlowBubbleItem> = listOf(
            FlowBubbleItem(1, "🔮", "DJ Set", "Infinito"),
            FlowBubbleItem(2, "🎭", "Ánimo", "Alegre / Fiesta"),
            FlowBubbleItem(3, "🎧", "Géneros", "Salsa / Reguetón"),
            FlowBubbleItem(4, "🔀", "Playlist DJ", "Smart Camelot"),
            FlowBubbleItem(5, "⚙️", "Automix", "DJ Toggle")
        )

        fun Song.toAutomixSongEntity(): code.name.monkey.retromusic.db.SongEntity {
            return code.name.monkey.retromusic.db.SongEntity(
                playlistCreatorId = -1L,
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

        fun showMoodDialog(context: Context) {
            val moods = AutomixRadioEngine.MoodType.values()
            val labels = moods.map { it.label }.toTypedArray()

            AlertDialog.Builder(context)
                .setTitle("Elige tu Estado de Ánimo 🎭")
                .setItems(labels) { _, which ->
                    val selectedMood = moods[which]
                    CoroutineScope(Dispatchers.IO).launch {
                        val songRepo = GlobalContext.get().get<SongRepository>()
                        val songs = songRepo.songs()
                        val entities = songs.map { it.toAutomixSongEntity() }
                        withContext(Dispatchers.Main) {
                            if (entities.isNotEmpty()) {
                                AutomixRadioEngine.getInstance(context).startMoodRadio(selectedMood, entities)
                                Toast.makeText(context, "Radio Ánimo iniciada: ${selectedMood.label}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Biblioteca vacía para mezclar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        fun showGenreDialog(context: Context) {
            val genres = arrayOf(
                "Latino / Salsa / Bomba",
                "Reguetón / Dembow / Trap",
                "K-Pop",
                "Rock",
                "Pop / Pop Latino",
                "Rap / Hip-Hop"
            )

            AlertDialog.Builder(context)
                .setTitle("Elige tu Género DJ 🎧")
                .setItems(genres) { _, which ->
                    val selectedGenre = genres[which]
                    CoroutineScope(Dispatchers.IO).launch {
                        val songRepo = GlobalContext.get().get<SongRepository>()
                        val songs = songRepo.songs()
                        val entities = songs.map { it.toAutomixSongEntity() }
                        withContext(Dispatchers.Main) {
                            if (entities.isNotEmpty()) {
                                AutomixRadioEngine.getInstance(context).startGenreRadio(selectedGenre, entities)
                                Toast.makeText(context, "Radio Género iniciada: $selectedGenre", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Biblioteca vacía para mezclar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
