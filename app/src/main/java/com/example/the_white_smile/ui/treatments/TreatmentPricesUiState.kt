package com.example.the_white_smile.ui.treatments

import com.example.the_white_smile.domain.Treatment

data class TreatmentPricesUiState(
    val treatments: List<Treatment> = emptyList(),
    val isAdmin: Boolean? = null,
    val savingTreatmentId: String? = null
)
