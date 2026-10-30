package com.example.the_white_smile.ui.treatments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.R
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AndroidConnectivityChecker
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.TreatmentPricingRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.UserRepository
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.domain.UserRole
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TreatmentPricesViewModel(
    treatmentRepository: TreatmentRepository,
    private val pricingRepository: TreatmentPricingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val connectivity: AndroidConnectivityChecker
) : ViewModel() {

    private val catalog = treatmentRepository.getTreatments()
    private val _uiState = MutableStateFlow(TreatmentPricesUiState(treatments = catalog))
    val uiState: StateFlow<TreatmentPricesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TreatmentPricesEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<TreatmentPricesEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid
            val profile = uid?.let { userRepository.getProfile(it).getOrNull() }
            val isAdmin = profile?.role == UserRole.ADMIN
            _uiState.update { it.copy(isAdmin = isAdmin) }
            if (!isAdmin) return@launch

            pricingRepository.observePrices()
                .catch { _events.emit(TreatmentPricesEvent.Error(it)) }
                .collect { prices ->
                    _uiState.update { state ->
                        state.copy(treatments = withPrices(prices))
                    }
                }
        }
    }

    fun savePrice(treatment: Treatment, priceCents: Long) {
        if (_uiState.value.isAdmin != true) {
            notify(R.string.treatment_prices_admin_only)
            return
        }
        if (priceCents <= 0L) {
            notify(R.string.treatment_prices_invalid)
            return
        }
        if (!connectivity.isOnline()) {
            notify(R.string.error_no_connection)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(savingTreatmentId = treatment.id) }
            pricingRepository.setPrice(treatment.id, priceCents)
                .onSuccess { notify(R.string.treatment_prices_saved) }
                .onFailure { _events.emit(TreatmentPricesEvent.Error(it)) }
            _uiState.update { it.copy(savingTreatmentId = null) }
        }
    }

    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    private fun withPrices(prices: Map<String, Long>): List<Treatment> =
        catalog.map { treatment ->
            treatment.copy(priceCents = prices[treatment.id] ?: treatment.priceCents)
        }

    private fun notify(messageRes: Int) {
        _events.tryEmit(TreatmentPricesEvent.Message(messageRes))
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TreatmentPricesViewModel(
                    treatmentRepository = ServiceLocator.treatmentRepository,
                    pricingRepository = ServiceLocator.treatmentPricingRepository,
                    authRepository = ServiceLocator.authRepository,
                    userRepository = ServiceLocator.userRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
