package com.example.the_white_smile.ui.appointments

/**
 * Horas disponibles para una combinacion de dentista, tratamiento y dia.
 * [times] contiene las marcas de tiempo que se pueden elegir.
 */
data class AvailabilityUiState(
    val dentistId: String,
    val treatmentId: String,
    val dayStartAt: Long,
    val times: List<Long> = emptyList(),
    val isLoading: Boolean = true
)
