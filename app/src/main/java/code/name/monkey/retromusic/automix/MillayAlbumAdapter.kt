package code.name.monkey.retromusic.automix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class MillayAlbumAdapter(
    private val albums: List<Map<String, String>>,
    private val onClick: (Map<String, String>) -> Unit
) : RecyclerView.Adapter<MillayAlbumAdapter.AlbumViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_millay_album_card, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albums[position])
    }

    override fun getItemCount() = albums.size

    inner class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val artView: ImageView = itemView.findViewById(R.id.millayAlbumArt)
        private val titleView: TextView = itemView.findViewById(R.id.millayAlbumTitle)
        private val artistView: TextView = itemView.findViewById(R.id.millayAlbumArtist)

        fun bind(album: Map<String, String>) {
            titleView.text = album["title"]
            artistView.text = album["artist"]

            Glide.with(artView.context)
                .load(album["cover"])
                .apply(
                    RequestOptions()
                        .transform(RoundedCorners(24))
                        .placeholder(R.drawable.millay_art_placeholder)
                        .error(R.drawable.millay_art_placeholder)
                )
                .into(artView)

            itemView.setOnClickListener { onClick(album) }
        }
    }
}
