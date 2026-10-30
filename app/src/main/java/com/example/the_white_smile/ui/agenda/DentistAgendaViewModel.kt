package com.example.the_white_smile.ui.agenda

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.R
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AndroidConnectivityChecker
import com.example.the_white_smile.data.AppointmentRepository
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.ConnectivityChecker
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
 * Logica de la agenda del dentista.
 * Observa las citas asignadas a la cuenta y permite marcarlas como
 * completadas cuando su hora ya paso.
 */
class DentistAgendaViewModel(
    private val repository: AppointmentRepository,
    private val authRepository: AuthRepository,
    private val connectivity: ConnectivityChecker,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(DentistAgendaUiState())
    val uiState: StateFlow<DentistAgendaUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DentistAgendaEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DentistAgendaEvent> = _events.asSharedFlow()

    init {
        observeAgenda()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    /**
     * Marca como completada una cita pendiente que ya ocurrio.
     * Las citas antiguas sin paciente no se pueden completar porque no se
     * conoce la ruta de su documento en Firestore.
     */
    fun completeAppointment(appointment: Appointment) {
        if (_uiState.value.isLoading) return
        if (appointment.status != AppointmentStatus.PENDING) return

        if (appointment.patientId == null) {
            notify(R.string.agenda_error_legacy)
            return
        }
        if (appointment.startAt > now()) {
            notify(R.string.agenda_error_future)
            return
        }
        if (!isOnline()) return

        runOperation(R.string.loading_complete_appointment) {
            repository.completeAppointment(appointment)
                .onSuccess { notify(R.string.agenda_completed) }
                .onFailure { _events.emit(DentistAgendaEvent.Error(it)) }
        }
    }

    fun saveSuggestion(appointment: Appointment, suggestion: String) {
        if (appointment.dentistId != authRepository.currentUser?.uid) return
        if (appointment.patientId == null) {
            notify(R.string.agenda_error_legacy)
            return
        }
        if (!isOnline()) return
        runOperation(R.string.loading_complete_appointment) {
            repository.updateDentistSuggestion(appointment, suggestion)
                .onSuccess { notify(R.string.agenda_suggestion_saved) }
                .onFailure { _events.emit(DentistAgendaEvent.Error(it)) }
        }
    }

    private fun observeAgenda() {
        val dentistId = authRepository.currentUser?.uid
        if (dentistId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            repository.observeDentistAppointments(dentistId)
                .catch { cause ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(DentistAgendaEvent.Error(cause))
                }
                .collect { appointments ->
                    _uiState.update {
                        it.copy(
                            appointments = appointments.sortedBy { appointment ->
                                appointment.startAt
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun isOnline(): Boolean {
        if (connectivity.isOnline()) return true
        notify(R.string.error_no_connection)
        return false
    }

    private fun runOperation(@StringRes messageRes: Int, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = messageRes)
            }
            try {
                block()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun notify(@StringRes messageRes: Int) {
        _events.tryEmit(DentistAgendaEvent.Message(messageRes))
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DentistAgendaViewModel(
                    repository = ServiceLocator.appointmentRepository,
                    authRepository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
