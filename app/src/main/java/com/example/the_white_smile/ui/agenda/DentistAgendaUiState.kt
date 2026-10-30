package com.example.the_white_smile.ui.agenda

import androidx.annotation.StringRes
import com.example.the_white_smile.R
import com.example.the_white_smile.domain.Appointment

/** Estado de la pantalla de agenda del dentista. */
data class DentistAgendaUiState(
    val appointments: List<Appointment> = emptyList(),
    val isLoading: Boolean = true,
    @param:StringRes val loadingMessage: Int = R.string.loading_agenda
) {
    /** Solo se muestra el estado vacio cuando termino la carga. */
    val isEmpty: Boolean get() = !isLoading && appointments.isEmpty()
}
