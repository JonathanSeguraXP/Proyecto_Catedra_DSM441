package com.example.the_white_smile.ui.treatments

import com.example.the_white_smile.domain.Treatment

/** Estado de la pantalla de detalle de un tratamiento. */
data class TreatmentDetailUiState(
    val treatment: Treatment? = null,
    val isAdmin: Boolean = false
)
