package com.badal.medtrack

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HomeAction(val label: String, val colorRes: Int, val onClick: () -> Unit)

class HomeCarouselAdapter(private val actions: List<HomeAction>) :
    RecyclerView.Adapter<HomeCarouselAdapter.PageViewHolder>() {

    private val itemsPerPage = 4
    private val pageCount = (actions.size + itemsPerPage - 1) / itemsPerPage
    private val virtualCount = if (pageCount > 1) pageCount * 1000 else pageCount

    class PageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val cards = listOf<TextView>(
            itemView.findViewById(R.id.card0),
            itemView.findViewById(R.id.card1),
            itemView.findViewById(R.id.card2),
            itemView.findViewById(R.id.card3)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_carousel_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val realPage = position % pageCount
        val startIndex = realPage * itemsPerPage

        for (i in 0 until itemsPerPage) {
            val actionIndex = startIndex + i
            val card = holder.cards[i]
            if (actionIndex < actions.size) {
                val action = actions[actionIndex]
                card.visibility = android.view.View.VISIBLE
                card.text = action.label
                card.setTextColor(card.context.getColor(action.colorRes))
                card.setOnClickListener { action.onClick() }
            } else {
                card.visibility = android.view.View.INVISIBLE
                card.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = virtualCount

    fun getRealPageCount(): Int = pageCount

    fun getStartPosition(): Int = if (pageCount > 1) (virtualCount / 2) - ((virtualCount / 2) % pageCount) else 0
}
