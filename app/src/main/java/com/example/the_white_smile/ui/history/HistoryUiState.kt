package com.example.the_white_smile.ui.history

import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.Treatment

/** Estado de la pantalla de historial de citas. */
data class HistoryUiState(
    val appointments: List<Appointment> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Solo se muestra el estado vacio cuando termino la carga. */
    val isEmpty: Boolean get() = !isLoading && appointments.isEmpty()
}
