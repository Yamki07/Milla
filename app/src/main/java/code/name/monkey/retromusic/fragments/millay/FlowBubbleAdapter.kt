/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.google.android.material.card.MaterialCardView

data class FlowBubble(
    val name: String,
    val subtitle: String,
    val colorHex: String,
    val emoji: String
)

/**
 * Adaptador de burbujas de Flow (Mood / Género) ReFreezer.
 */
class FlowBubbleAdapter(
    private val bubbles: List<FlowBubble>,
    private val onBubbleClick: (FlowBubble) -> Unit
) : RecyclerView.Adapter<FlowBubbleAdapter.BubbleViewHolder>() {

    inner class BubbleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.bubbleCard)
        val emoji: TextView        = view.findViewById(R.id.bubbleEmoji)
        val title: TextView        = view.findViewById(R.id.bubbleTitle)
        val subtitle: TextView     = view.findViewById(R.id.bubbleSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flow_bubble_milla, parent, false)
        return BubbleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
        val bubble = bubbles[position]
        holder.emoji.text    = bubble.emoji
        holder.title.text    = bubble.name
        holder.subtitle.text = bubble.subtitle
        try {
            holder.card.setCardBackgroundColor(Color.parseColor(bubble.colorHex))
        } catch (e: Exception) {}

        holder.itemView.setOnClickListener { onBubbleClick(bubble) }
    }

    override fun getItemCount(): Int = bubbles.size
}
