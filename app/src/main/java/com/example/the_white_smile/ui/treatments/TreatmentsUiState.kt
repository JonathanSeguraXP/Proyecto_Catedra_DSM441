package com.example.the_white_smile.ui.treatments

import com.example.the_white_smile.domain.Treatment

/** Estado de la pantalla del catalogo de tratamientos. */
data class TreatmentsUiState(
    val treatments: List<Treatment> = emptyList()
) {
    val isEmpty: Boolean get() = treatments.isEmpty()
}
