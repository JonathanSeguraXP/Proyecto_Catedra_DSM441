package com.example.the_white_smile.ui.appointments

import androidx.annotation.StringRes
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.domain.Treatment

/** Estado de la pantalla de citas. */
data class AppointmentsUiState(
    val appointments: List<Appointment> = emptyList(),
    val dentists: List<Dentist> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val availability: AvailabilityUiState? = null,
    val isLoading: Boolean = false,
    @param:StringRes val loadingMessage: Int = R.string.loading_appointments
) {
    val isEmpty: Boolean get() = appointments.isEmpty()
}
