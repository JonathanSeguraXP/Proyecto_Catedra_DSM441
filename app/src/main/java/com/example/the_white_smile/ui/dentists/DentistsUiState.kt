package com.example.the_white_smile.ui.dentists

import com.example.the_white_smile.domain.Dentist

/** Estado de la pantalla de administracion de dentistas. */
data class DentistsUiState(
    val dentists: List<Dentist> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false
) {
    val isEmpty: Boolean get() = !isLoading && dentists.isEmpty()
}
