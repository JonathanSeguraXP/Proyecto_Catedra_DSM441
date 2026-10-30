package com.example.the_white_smile.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.TreatmentPricingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Logica de la pantalla de detalle de un tratamiento. */
class TreatmentDetailViewModel(
    repository: TreatmentRepository,
    treatmentId: String,
    pricingRepository: TreatmentPricingRepository? = null
) : ViewModel() {

    private val baseTreatment = repository.getTreatment(treatmentId)
    private val _uiState = MutableStateFlow(
        TreatmentDetailUiState(treatment = baseTreatment)
    )
    val uiState: StateFlow<TreatmentDetailUiState> = _uiState.asStateFlow()

    init {
        if (baseTreatment != null && pricingRepository != null) {
            viewModelScope.launch {
                pricingRepository.observePrices()
                    .catch { }
                    .collect { prices ->
                        _uiState.value = TreatmentDetailUiState(
                            treatment = baseTreatment.copy(
                                priceCents = prices[baseTreatment.id] ?: baseTreatment.priceCents
                            )
                        )
                    }
            }
        }
    }

    companion object {
        fun factory(treatmentId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TreatmentDetailViewModel(
                    repository = ServiceLocator.treatmentRepository,
                    treatmentId = treatmentId,
                    pricingRepository = ServiceLocator.treatmentPricingRepository
                )
            }
        }
    }
}
