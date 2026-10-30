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
import com.google.android.material.button.MaterialButton

/** Adaptador de la agenda del dentista. */
class DentistAgendaAdapter(
    private val onComplete: (Appointment) -> Unit
) : ListAdapter<Appointment, DentistAgendaAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dentist_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onComplete)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvPatient: TextView = itemView.findViewById(R.id.tvAgendaPatient)
        private val tvService: TextView = itemView.findViewById(R.id.tvAgendaService)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvAgendaDateTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvAgendaDuration)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAgendaStatus)
        private val btnComplete: MaterialButton =
            itemView.findViewById(R.id.btnCompleteAppointment)

        fun bind(appointment: Appointment, onComplete: (Appointment) -> Unit) {
            val context = itemView.context

            val patientName = appointment.patientName?.trim().orEmpty()
            tvPatient.text = if (patientName.isEmpty()) {
                context.getString(R.string.agenda_patient_unknown)
            } else {
                context.getString(R.string.agenda_patient_label, patientName)
            }
            tvService.text = appointment.service
            tvDateTime.text = DateFormats.dateTime(appointment.startAt)
            tvDuration.text = context.getString(
                R.string.appointments_duration,
                appointment.durationMinutes
            )

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
        }
    }

    private object Diff : DiffUtil.ItemCallback<Appointment>() {

        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem == newItem
    }
}
