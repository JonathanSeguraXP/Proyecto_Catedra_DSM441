package com.example.the_white_smile.ui.dentists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Dentist
import com.google.android.material.materialswitch.MaterialSwitch

/** Adaptador de la lista de dentistas del administrador. */
class DentistAdapter(
    private val onActiveChanged: (Dentist, Boolean) -> Unit
) : ListAdapter<Dentist, DentistAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dentist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onActiveChanged)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvDentistName)
        private val tvSpecialty: TextView = itemView.findViewById(R.id.tvDentistSpecialty)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvDentistEmail)
        private val switchActive: MaterialSwitch = itemView.findViewById(R.id.switchDentistActive)

        fun bind(dentist: Dentist, onActiveChanged: (Dentist, Boolean) -> Unit) {
            tvName.text = dentist.displayName
            tvSpecialty.text = dentist.specialty
            tvEmail.text = dentist.email.orEmpty()

            // Se quita el listener al reciclar para no disparar cambios falsos.
            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = dentist.active
            switchActive.setText(
                if (dentist.active) R.string.dentists_active
                else R.string.dentists_inactive
            )
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                onActiveChanged(dentist, isChecked)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Dentist>() {

        override fun areItemsTheSame(oldItem: Dentist, newItem: Dentist): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Dentist, newItem: Dentist): Boolean =
            oldItem == newItem
    }
}
