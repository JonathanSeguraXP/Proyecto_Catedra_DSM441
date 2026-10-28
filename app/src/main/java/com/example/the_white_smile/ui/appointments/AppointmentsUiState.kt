package com.example.the_white_smile.ui.appointments

import androidx.annotation.StringRes
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Appointment

/** Estado de la pantalla de citas. */
data class AppointmentsUiState(
    val appointments: List<Appointment> = emptyList(),
    val isLoading: Boolean = false,
    @param:StringRes val loadingMessage: Int = R.string.loading_appointments
) {
    val isEmpty: Boolean get() = appointments.isEmpty()
}
