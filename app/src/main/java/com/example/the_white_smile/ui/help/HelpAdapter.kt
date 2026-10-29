package com.example.the_white_smile.ui.help

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.R

/** Adaptador de las preguntas frecuentes desplegables. */
class HelpAdapter(
    private val onToggle: (FaqItem) -> Unit
) : ListAdapter<FaqEntry, HelpAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onToggle)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvQuestion: TextView = itemView.findViewById(R.id.tvFaqQuestion)
        private val tvAnswer: TextView = itemView.findViewById(R.id.tvFaqAnswer)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivFaqExpand)

        fun bind(entry: FaqEntry, onToggle: (FaqItem) -> Unit) {
            tvQuestion.setText(entry.faq.questionRes)
            tvAnswer.setText(entry.faq.answerRes)
            tvAnswer.visibility = if (entry.isExpanded) View.VISIBLE else View.GONE
            ivExpand.rotation = if (entry.isExpanded) 180f else 0f
            itemView.setOnClickListener { onToggle(entry.faq) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FaqEntry>() {

        override fun areItemsTheSame(oldItem: FaqEntry, newItem: FaqEntry): Boolean =
            oldItem.faq.id == newItem.faq.id

        override fun areContentsTheSame(oldItem: FaqEntry, newItem: FaqEntry): Boolean =
            oldItem == newItem
    }
}
