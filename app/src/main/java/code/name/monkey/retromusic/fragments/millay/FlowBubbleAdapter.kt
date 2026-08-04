/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.millay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.google.android.material.card.MaterialCardView

/**
 * Adaptador de burbujas de Flow (Mood / Género) para la pantalla de Inicio de Millay.
 * Muestra un grid horizontal de íconos circulares con emoji, título y subtítulo.
 */
class FlowBubbleAdapter(
    private val bubbles: List<MillayHomeFragment.FlowBubble>,
    private val onBubbleClick: (MillayHomeFragment.FlowBubble) -> Unit
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
        holder.title.text    = bubble.title
        holder.subtitle.text = bubble.subtitle
        holder.itemView.setOnClickListener { onBubbleClick(bubble) }
    }

    override fun getItemCount(): Int = bubbles.size
}
