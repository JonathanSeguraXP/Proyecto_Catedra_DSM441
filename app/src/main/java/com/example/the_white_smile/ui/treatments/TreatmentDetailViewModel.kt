package com.example.the_white_smile.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.TreatmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Logica de la pantalla de detalle de un tratamiento. */
class TreatmentDetailViewModel(
    repository: TreatmentRepository,
    treatmentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TreatmentDetailUiState(treatment = repository.getTreatment(treatmentId))
    )
    val uiState: StateFlow<TreatmentDetailUiState> = _uiState.asStateFlow()

    companion object {
        fun factory(treatmentId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TreatmentDetailViewModel(
                    repository = ServiceLocator.treatmentRepository,
                    treatmentId = treatmentId
                )
            }
        }
    }
}
