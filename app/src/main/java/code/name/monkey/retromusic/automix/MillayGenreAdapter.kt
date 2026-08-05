package code.name.monkey.retromusic.automix

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R

class MillayGenreAdapter(
    private val genres: List<Map<String, String>>,
    private val onClick: (Map<String, String>) -> Unit
) : RecyclerView.Adapter<MillayGenreAdapter.GenreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_millay_genre_card, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.bind(genres[position])
    }

    override fun getItemCount() = genres.size

    inner class GenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView as CardView
        private val titleView: TextView = itemView.findViewById(R.id.millayGenreTitle)

        fun bind(genre: Map<String, String>) {
            titleView.text = genre["name"]
            
            try {
                cardView.setCardBackgroundColor(Color.parseColor(genre["color"]))
            } catch (e: Exception) {
                cardView.setCardBackgroundColor(Color.parseColor("#2A2D3E"))
            }

            itemView.setOnClickListener { onClick(genre) }
        }
    }
}
