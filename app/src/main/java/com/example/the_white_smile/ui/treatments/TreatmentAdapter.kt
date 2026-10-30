package com.example.the_white_smile.ui.treatments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.util.ProfilePhotoCodec

/** Adaptador del catalogo de tratamientos. */
class TreatmentAdapter(
    private val onClick: (Treatment) -> Unit
) : ListAdapter<Treatment, TreatmentAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_treatment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvTreatmentName)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvTreatmentSummary)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvTreatmentDuration)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvTreatmentPrice)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivTreatmentPhoto)

        fun bind(treatment: Treatment, onClick: (Treatment) -> Unit) {
            tvName.text = treatment.name
            ProfilePhotoCodec.decode(treatment.profilePhotoBase64)?.let(ivPhoto::setImageBitmap)
                ?: ivPhoto.setImageResource(android.R.drawable.ic_menu_agenda)
            tvSummary.text = treatment.summary
            tvDuration.text = itemView.context.getString(
                R.string.treatments_duration_short,
                treatment.durationMinutes
            )
            tvPrice.text = treatment.priceCents?.let {
                itemView.context.getString(
                    R.string.treatments_price_label,
                    com.example.the_white_smile.util.MoneyFormats.usd(it)
                )
            } ?: itemView.context.getString(R.string.treatments_price_unconfigured)
            itemView.setOnClickListener { onClick(treatment) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Treatment>() {

        override fun areItemsTheSame(oldItem: Treatment, newItem: Treatment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Treatment, newItem: Treatment): Boolean =
            oldItem == newItem
    }
}
