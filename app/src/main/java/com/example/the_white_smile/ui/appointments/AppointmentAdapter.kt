package com.example.the_white_smile.ui.appointments

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

/** Adaptador de la lista de citas. */
class AppointmentAdapter(
    private val onCancel: (Appointment) -> Unit
) : ListAdapter<Appointment, AppointmentAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onCancel)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvService: TextView = itemView.findViewById(R.id.tvAppointmentService)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvAppointmentDateTime)
        private val tvDentist: TextView = itemView.findViewById(R.id.tvAppointmentDentist)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvAppointmentPrice)
        private val tvDeposit: TextView = itemView.findViewById(R.id.tvAppointmentDeposit)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvAppointmentStatus)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancelAppointment)

        fun bind(appointment: Appointment, onCancel: (Appointment) -> Unit) {
            val context = itemView.context

            tvService.text = appointment.service
            tvDateTime.text = DateFormats.dateTime(appointment.startAt)

            val dentistName = appointment.dentistName?.trim().orEmpty()
            tvDentist.visibility = if (dentistName.isEmpty()) View.GONE else View.VISIBLE
            tvDentist.text = context.getString(R.string.appointments_dentist_label, dentistName)

            tvPrice.visibility = if (appointment.totalPriceCents == null) View.GONE else View.VISIBLE
            if (appointment.totalPriceCents != null) {
                tvPrice.text = context.getString(
                    R.string.treatments_price_label,
                    MoneyFormats.usd(appointment.totalPriceCents)
                )
            }
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

            val canCancel = appointment.status == AppointmentStatus.PENDING &&
                appointment.startAt > System.currentTimeMillis()
            btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE
            btnCancel.setOnClickListener { onCancel(appointment) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Appointment>() {

        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean =
            oldItem == newItem
    }
}
