package com.example.the_white_smile.ui.appointments

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
 * Logica de la pantalla de citas.
 * Observa los turnos del usuario y permite agendar y cancelar.
 */
class AppointmentsViewModel(
    private val repository: AppointmentRepository,
    private val authRepository: AuthRepository,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppointmentsEvent> = _events.asSharedFlow()

    init {
        observeAppointments()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    fun createAppointment(service: String, startAt: Long) {
        if (_uiState.value.isLoading) return

        val cleanService = service.trim()
        when {
            cleanService.isEmpty() -> {
                notify(R.string.appointments_error_service)
                return
            }
            startAt <= System.currentTimeMillis() -> {
                notify(R.string.appointments_error_past)
                return
            }
            hasConflict(startAt) -> {
                notify(R.string.appointments_error_conflict)
                return
            }
            !isOnline() -> return
        }

        runOperation(R.string.loading_saving_appointment) {
            repository.createAppointment(cleanService, startAt)
                .onSuccess { notify(R.string.appointment_created) }
                .onFailure { _events.emit(AppointmentsEvent.Error(it)) }
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        if (_uiState.value.isLoading) return
        if (appointment.status != AppointmentStatus.PENDING) return
        if (!isOnline()) return

        runOperation(R.string.loading_cancel_appointment) {
            repository.cancelAppointment(appointment.id)
                .onSuccess { notify(R.string.appointment_cancelled) }
                .onFailure { _events.emit(AppointmentsEvent.Error(it)) }
        }
    }

    private fun observeAppointments() {
        viewModelScope.launch {
            repository.observeAppointments()
                .catch { _events.emit(AppointmentsEvent.Error(it)) }
                .collect { appointments ->
                    _uiState.update { it.copy(appointments = appointments) }
                }
        }
    }

    /** No se permiten dos turnos pendientes a la misma hora. */
    private fun hasConflict(startAt: Long): Boolean =
        _uiState.value.appointments.any {
            it.status == AppointmentStatus.PENDING && it.startAt == startAt
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
        _events.tryEmit(AppointmentsEvent.Message(messageRes))
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppointmentsViewModel(
                    repository = ServiceLocator.appointmentRepository,
                    authRepository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
