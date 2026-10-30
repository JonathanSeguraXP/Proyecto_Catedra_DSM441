package com.example.the_white_smile.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.TreatmentPricingRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Logica de la pantalla con el catalogo de tratamientos. */
class TreatmentsViewModel(
    repository: TreatmentRepository,
    pricingRepository: TreatmentPricingRepository? = null
) : ViewModel() {

    private val catalog = repository.getTreatments()
    private val _uiState = MutableStateFlow(
        TreatmentsUiState(treatments = catalog)
    )
    val uiState: StateFlow<TreatmentsUiState> = _uiState.asStateFlow()

    init {
        pricingRepository?.let { pricing ->
            viewModelScope.launch {
                pricing.observePrices()
                    .catch { }
                    .collect { prices ->
                        _uiState.value = TreatmentsUiState(
                            catalog.map { treatment ->
                                treatment.copy(priceCents = prices[treatment.id] ?: treatment.priceCents)
                            }
                        )
                    }
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TreatmentsViewModel(
                    repository = ServiceLocator.treatmentRepository,
                    pricingRepository = ServiceLocator.treatmentPricingRepository
                )
            }
        }
    }
}
