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

/** Logica de la pantalla con el catalogo de tratamientos. */
class TreatmentsViewModel(
    repository: TreatmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TreatmentsUiState(treatments = repository.getTreatments())
    )
    val uiState: StateFlow<TreatmentsUiState> = _uiState.asStateFlow()

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TreatmentsViewModel(ServiceLocator.treatmentRepository)
            }
        }
    }
}
