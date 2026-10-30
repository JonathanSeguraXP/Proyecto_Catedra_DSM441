package com.example.the_white_smile.ui.agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.util.DateFormats
import com.example.the_white_smile.util.MoneyFormats
import com.google.android.material.button.MaterialButton

/** Adaptador de la agenda del dentista. */
class DentistAgendaAdapter(
    private val onComplete: (Appointment) -> Unit,
    private val onSuggest: (Appointment) -> Unit
) : ListAdapter<Appointment, DentistAgendaAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dentist_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onComplete, onSuggest)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvPatient: TextView = itemView.findViewById(R.id.tvAgendaPatient)
        private val tvService: TextView = itemView.findViewById(R.id.tvAgendaService)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvAgendaDateTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvAgendaDuration)
        private val tvDeposit: TextView = itemView.findViewById(R.id.tvAgendaDeposit)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAgendaStatus)
        private val tvSuggestion: TextView = itemView.findViewById(R.id.tvAgendaSuggestion)
        private val btnSuggest: MaterialButton = itemView.findViewById(R.id.btnSuggestAppointment)
        private val btnComplete: MaterialButton =
            itemView.findViewById(R.id.btnCompleteAppointment)

        fun bind(appointment: Appointment, onComplete: (Appointment) -> Unit, onSuggest: (Appointment) -> Unit) {
            val context = itemView.context

            val patientName = appointment.patientName?.trim().orEmpty()
            tvPatient.text = if (patientName.isEmpty()) {
                context.getString(R.string.agenda_patient_unknown)
            } else {
                context.getString(R.string.agenda_patient_label, patientName)
            }
            tvService.text = appointment.service
            btnSuggest.setText(
                if (appointment.dentistSuggestion.isNullOrBlank()) R.string.agenda_suggestion_action
                else R.string.agenda_suggestion_edit
            )
            tvSuggestion.visibility = appointment.dentistSuggestion?.takeIf { it.isNotBlank() }
                ?.let { tvSuggestion.text = context.getString(R.string.agenda_suggestion, it); View.VISIBLE }
                ?: View.GONE
            tvDateTime.text = DateFormats.dateTime(appointment.startAt)
            tvDuration.text = context.getString(
                R.string.appointments_duration,
                appointment.durationMinutes
            )
            tvDeposit.visibility = if (appointment.depositCents == null) View.GONE else View.VISIBLE
            if (appointment.depositCents != null && appointment.totalPriceCents != null) {
                tvDeposit.text = context.getString(
                    R.string.appointments_deposit_pending,
                    MoneyFormats.usd(appointment.depositCents),
                    MoneyFormats.usd(appointment.totalPriceCents)
                )
            }

            tvStatus.setText(
                when (appointment.status) {
                    AppointmentStatus.PENDING -> R.string.appointments_status_pending
                    AppointmentStatus.CANCELLED -> R.string.appointments_status_cancelled
                    AppointmentStatus.COMPLETED -> R.string.appointments_status_completed
                }
            )
            tvStatus.setTextColor(
                ContextCompat.getColor(
                    context,
                    when (appointment.status) {
                        AppointmentStatus.PENDING -> R.color.secondary
                        AppointmentStatus.CANCELLED -> R.color.on_surface_variant
                        AppointmentStatus.COMPLETED -> R.color.tertiary
                    }
                )
            )

            val canComplete = appointment.status == AppointmentStatus.PENDING &&
                appointment.patientId != null &&
                appointment.startAt <= System.currentTimeMillis()
            btnComplete.visibility = if (canComplete) View.VISIBLE else View.GONE
            btnComplete.setOnClickListener { onComplete(appointment) }
            btnSuggest.setOnClickListener { onSuggest(appointment) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Appointment>() {

        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem == newItem
    }
}
