package com.example.the_white_smile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AppointmentRepository
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.domain.AppointmentStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logica de la pantalla de historial.
 * Muestra las citas que ya ocurrieron o que fueron canceladas, de la mas
 * reciente a la mas antigua, reutilizando el repositorio de citas.
 */
class HistoryViewModel(
    private val repository: AppointmentRepository,
    private val authRepository: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val treatmentRepository: TreatmentRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    init {
        observeHistory()
        treatmentRepository?.let { treatments ->
            viewModelScope.launch {
                treatments.observeCatalog().catch { }.collect { catalog ->
                    _uiState.update { it.copy(treatments = catalog) }
                }
            }
        }
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    private fun observeHistory() {
        viewModelScope.launch {
            repository.observeAppointments()
                .catch { cause ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(HistoryEvent.Error(cause))
                }
                .collect { appointments ->
                    val currentTime = now()
                    _uiState.update {
                        it.copy(
                            appointments = appointments
                                .filter { appointment ->
                                    appointment.status != AppointmentStatus.PENDING ||
                                        appointment.startAt <= currentTime
                                }
                                .sortedByDescending { appointment -> appointment.startAt },
                            isLoading = false
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(
                    repository = ServiceLocator.appointmentRepository,
                    authRepository = ServiceLocator.authRepository,
                    treatmentRepository = ServiceLocator.treatmentRepository
                )
            }
        }
    }
}
