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
import com.example.the_white_smile.data.DentistRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentSchedule
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.SlotConflictException
import kotlinx.coroutines.Job
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
 * Observa los turnos del usuario, calcula las horas libres de cada dentista
 * y permite agendar y cancelar.
 */
class AppointmentsViewModel(
    private val repository: AppointmentRepository,
    private val dentistRepository: DentistRepository,
    private val treatmentRepository: TreatmentRepository,
    private val authRepository: AuthRepository,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppointmentsUiState(treatments = treatmentRepository.getTreatments())
    )
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppointmentsEvent> = _events.asSharedFlow()

    /** Escucha activa de los bloques ocupados del dia elegido. */
    private var availabilityJob: Job? = null

    init {
        observeAppointments()
        observeDentists()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    /**
     * Carga las horas libres del dentista para un tratamiento y un dia.
     * Se vuelve a calcular automaticamente si otro paciente ocupa un bloque.
     */
    fun requestAvailability(dentistId: String, treatmentId: String, dayStartAt: Long) {
        val state = _uiState.value
        val dentist = state.dentists.firstOrNull { it.id == dentistId }
        val treatment = state.treatments.firstOrNull { it.id == treatmentId }
        if (dentist == null || treatment == null) {
            clearAvailability()
            return
        }

        availabilityJob?.cancel()
        _uiState.update {
            it.copy(
                availability = AvailabilityUiState(
                    dentistId = dentistId,
                    treatmentId = treatmentId,
                    dayStartAt = dayStartAt,
                    isLoading = true
                )
            )
        }

        availabilityJob = viewModelScope.launch {
            repository.observeBusySlots(
                dentistId = dentistId,
                dayStartAt = dayStartAt,
                dayEndAt = AppointmentSchedule.endOfDay(dayStartAt)
            )
                .catch { cause ->
                    _uiState.update { it.copy(availability = it.availability?.copy(isLoading = false)) }
                    _events.emit(AppointmentsEvent.Error(cause))
                }
                .collect { busySlots ->
                    val times = AppointmentSchedule.availableStarts(
                        dentist = dentist,
                        dayStartAt = dayStartAt,
                        durationMinutes = treatment.durationMinutes,
                        busySlots = busySlots,
                        notBefore = System.currentTimeMillis()
                    )
                    _uiState.update { current ->
                        val availability = current.availability
                        if (availability == null ||
                            availability.dentistId != dentistId ||
                            availability.treatmentId != treatmentId ||
                            availability.dayStartAt != dayStartAt
                        ) {
                            current
                        } else {
                            current.copy(
                                availability = availability.copy(
                                    times = times,
                                    isLoading = false
                                )
                            )
                        }
                    }
                }
        }
    }

    /** Descarta la disponibilidad mostrada al cerrar el dialogo. */
    fun clearAvailability() {
        availabilityJob?.cancel()
        availabilityJob = null
        _uiState.update { it.copy(availability = null) }
    }

    fun createAppointment(treatmentId: String, startAt: Long, dentistId: String) {
        if (_uiState.value.isLoading) return

        val state = _uiState.value
        val treatment = state.treatments.firstOrNull { it.id == treatmentId }
        val dentist = state.dentists.firstOrNull { it.id == dentistId }

        when {
            treatment == null -> {
                notify(R.string.appointments_error_treatment)
                return
            }
            dentist == null -> {
                notify(R.string.appointments_error_dentist)
                return
            }
            startAt <= System.currentTimeMillis() -> {
                notify(R.string.appointments_error_past)
                return
            }
            !AppointmentSchedule.isWithinSchedule(
                dentist,
                startAt,
                treatment.durationMinutes
            ) -> {
                notify(R.string.appointments_error_schedule)
                return
            }
            hasConflict(startAt, treatment.durationMinutes) -> {
                notify(R.string.appointments_error_conflict)
                return
            }
            !isOnline() -> return
        }

        runOperation(R.string.loading_saving_appointment) {
            repository.createAppointment(
                service = treatment.name,
                startAt = startAt,
                treatmentId = treatment.id,
                durationMinutes = treatment.durationMinutes,
                dentistId = dentist.id,
                dentistName = dentist.displayName
            )
                .onSuccess {
                    clearAvailability()
                    notify(R.string.appointment_created)
                }
                .onFailure { cause ->
                    if (cause is SlotConflictException) {
                        notify(R.string.appointments_error_slot_taken)
                    } else {
                        _events.emit(AppointmentsEvent.Error(cause))
                    }
                }
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        if (_uiState.value.isLoading) return
        if (appointment.status != AppointmentStatus.PENDING) return
        if (!isOnline()) return

        runOperation(R.string.loading_cancel_appointment) {
            repository.cancelAppointment(appointment)
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

    /** Solo se ofrecen los dentistas activos al agendar. */
    private fun observeDentists() {
        viewModelScope.launch {
            dentistRepository.observeDentists()
                .catch { _events.emit(AppointmentsEvent.Error(it)) }
                .collect { dentists ->
                    val active = dentists.filter { it.active }
                    _uiState.update { state ->
                        state.copy(dentists = active)
                    }
                }
        }
    }

    /** No se permiten dos turnos pendientes que se crucen. */
    private fun hasConflict(startAt: Long, durationMinutes: Int): Boolean {
        val endAt = startAt + durationMinutes * 60_000L
        return _uiState.value.appointments.any { appointment ->
            if (appointment.status != AppointmentStatus.PENDING) return@any false
            val otherEnd = appointment.startAt +
                appointment.durationMinutes * 60_000L
            startAt < otherEnd && appointment.startAt < endAt
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
        _events.tryEmit(AppointmentsEvent.Message(messageRes))
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppointmentsViewModel(
                    repository = ServiceLocator.appointmentRepository,
                    dentistRepository = ServiceLocator.dentistRepository(context),
                    treatmentRepository = ServiceLocator.treatmentRepository,
                    authRepository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
