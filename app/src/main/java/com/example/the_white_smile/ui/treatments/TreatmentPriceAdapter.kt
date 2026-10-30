package com.example.the_white_smile.ui.treatments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.util.MoneyFormats
import com.google.android.material.button.MaterialButton

class TreatmentPriceAdapter(
    private val onEdit: (Treatment) -> Unit
) : ListAdapter<Treatment, TreatmentPriceAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_treatment_price, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onEdit)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tvPriceTreatmentName)
        private val value: TextView = itemView.findViewById(R.id.tvPriceTreatmentValue)
        private val button: MaterialButton = itemView.findViewById(R.id.btnEditTreatmentPrice)

        fun bind(treatment: Treatment, onEdit: (Treatment) -> Unit) {
            name.text = treatment.name
            val price = treatment.priceCents?.let(MoneyFormats::usd)
            value.text = if (price == null) {
                itemView.context.getString(R.string.treatment_prices_pending)
            } else {
                itemView.context.getString(R.string.treatment_prices_from_label, price)
            }
            button.setOnClickListener { onEdit(treatment) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Treatment>() {
        override fun areItemsTheSame(oldItem: Treatment, newItem: Treatment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Treatment, newItem: Treatment): Boolean =
            oldItem == newItem
    }
}
