package com.example.the_white_smile.ui.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.TreatmentPricingRepository
import com.example.the_white_smile.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Logica de la pantalla de detalle de un tratamiento. */
class TreatmentDetailViewModel(
    private val repository: TreatmentRepository,
    treatmentId: String,
    private val pricingRepository: TreatmentPricingRepository? = null
) : ViewModel() {

    private val baseTreatment = repository.getTreatment(treatmentId)
    private val _uiState = MutableStateFlow(
        TreatmentDetailUiState(treatment = baseTreatment)
    )
    val uiState: StateFlow<TreatmentDetailUiState> = _uiState.asStateFlow()

    suspend fun save(treatment: com.example.the_white_smile.domain.Treatment): Result<Unit> {
        val catalogResult = repository.saveTreatment(treatment)
        if (catalogResult.isFailure) return catalogResult

        val price = treatment.priceCents
        if (price != null && pricingRepository != null) {
            val priceResult = pricingRepository.setPrice(treatment.id, price)
            if (priceResult.isFailure) {
                return Result.failure(
                    priceResult.exceptionOrNull()
                        ?: IllegalStateException("No se pudo guardar el precio")
                )
            }
        }

        _uiState.value = _uiState.value.copy(treatment = treatment)
        return Result.success(Unit)
    }

    suspend fun delete(treatmentId: String): Result<Unit> =
        repository.deleteTreatment(treatmentId)

    init {
        if (baseTreatment != null) {
            viewModelScope.launch {
                repository.observeCatalog().catch { }.collect { catalog ->
                    catalog.firstOrNull { it.id == baseTreatment.id }?.let { treatment ->
                        _uiState.value = _uiState.value.copy(treatment = treatment)
                    }
                }
            }
        }
        if (baseTreatment != null && pricingRepository != null) {
            viewModelScope.launch {
                pricingRepository.observePrices()
                    .catch { }
                    .collect { prices ->
                        _uiState.value = _uiState.value.copy(
                            treatment = (_uiState.value.treatment ?: baseTreatment).copy(
                                priceCents = prices[baseTreatment.id] ?: baseTreatment.priceCents
                            )
                        )
                    }
            }
        }
        viewModelScope.launch {
            val profile = ServiceLocator.userRepository.getProfile(
                ServiceLocator.auth.currentUser?.uid.orEmpty()
            ).getOrNull()
            _uiState.value = _uiState.value.copy(isAdmin = profile?.role == UserRole.ADMIN)
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
