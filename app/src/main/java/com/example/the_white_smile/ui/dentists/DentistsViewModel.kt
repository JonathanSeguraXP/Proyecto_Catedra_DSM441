package com.example.the_white_smile.ui.dentists

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
import com.example.the_white_smile.data.AndroidEmailValidator
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.DentistRepository
import com.example.the_white_smile.domain.AppointmentSchedule
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.domain.EmailValidator
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
 * Logica de la pantalla de dentistas del administrador.
 * Lista los dentistas y permite crear sus cuentas y activarlas o
 * desactivarlas.
 */
class DentistsViewModel(
    private val repository: DentistRepository,
    private val authRepository: AuthRepository,
    private val emailValidator: EmailValidator,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(DentistsUiState())
    val uiState: StateFlow<DentistsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DentistsEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DentistsEvent> = _events.asSharedFlow()

    init {
        observeDentists()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = authRepository.isLoggedIn()

    fun createDentist(
        displayName: String,
        specialty: String,
        email: String,
        password: String,
        workDays: Set<Int> = Dentist.DEFAULT_WORK_DAYS,
        startMinute: Int = Dentist.DEFAULT_START_MINUTE,
        endMinute: Int = Dentist.DEFAULT_END_MINUTE
    ) {
        if (_uiState.value.isCreating) return

        val cleanName = displayName.trim()
        val cleanSpecialty = specialty.trim()
        val cleanEmail = email.trim()
        val scheduleError = scheduleError(workDays, startMinute, endMinute)

        when {
            cleanName.isEmpty() -> {
                notify(R.string.dentists_error_name)
                return
            }
            cleanSpecialty.isEmpty() -> {
                notify(R.string.dentists_error_specialty)
                return
            }
            cleanEmail.isEmpty() || !emailValidator.isValid(cleanEmail) -> {
                notify(R.string.dentists_error_email)
                return
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                notify(R.string.dentists_error_password)
                return
            }
            scheduleError != null -> {
                notify(scheduleError)
                return
            }
            !isOnline() -> return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                repository.createDentist(
                    displayName = cleanName,
                    email = cleanEmail,
                    password = password,
                    specialty = cleanSpecialty,
                    workDays = workDays,
                    startMinute = startMinute,
                    endMinute = endMinute
                )
                    .onSuccess { _events.emit(DentistsEvent.Created(it.displayName)) }
                    .onFailure { _events.emit(DentistsEvent.Error(it)) }
            } finally {
                _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    /** Actualiza los dias y el rango de atencion de un dentista existente. */
    fun updateSchedule(
        dentist: Dentist,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ) {
        if (_uiState.value.isCreating) return

        val scheduleError = scheduleError(workDays, startMinute, endMinute)
        when {
            scheduleError != null -> {
                notify(scheduleError)
                return
            }
            dentist.workDays == workDays &&
                dentist.startMinute == startMinute &&
                dentist.endMinute == endMinute -> {
                notify(R.string.dentists_schedule_unchanged)
                return
            }
            !isOnline() -> return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                repository.updateSchedule(
                    dentistId = dentist.id,
                    workDays = workDays,
                    startMinute = startMinute,
                    endMinute = endMinute
                )
                    .onSuccess {
                        _events.emit(DentistsEvent.ScheduleUpdated(dentist.displayName))
                    }
                    .onFailure { _events.emit(DentistsEvent.Error(it)) }
            } finally {
                _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    /** Activa o desactiva un dentista para que pueda recibir citas. */
    fun setActive(dentist: Dentist, active: Boolean) {
        if (dentist.active == active) return
        if (!isOnline()) return

        viewModelScope.launch {
            repository.setActive(dentist.id, active)
                .onSuccess {
                    notify(
                        if (active) R.string.dentists_activated
                        else R.string.dentists_deactivated
                    )
                }
                .onFailure { _events.emit(DentistsEvent.Error(it)) }
        }
    }

    private fun observeDentists() {
        viewModelScope.launch {
            repository.observeDentists()
                .catch { cause ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(DentistsEvent.Error(cause))
                }
                .collect { dentists ->
                    _uiState.update { it.copy(dentists = dentists, isLoading = false) }
                }
        }
    }

    private fun isOnline(): Boolean {
        if (connectivity.isOnline()) return true
        notify(R.string.error_no_connection)
        return false
    }

    /** Devuelve el mensaje de error del horario, o null si es valido. */
    @StringRes
    private fun scheduleError(
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Int? = when {
        workDays.isEmpty() -> R.string.dentists_error_work_days
        startMinute % AppointmentSchedule.SLOT_MINUTES != 0 ||
            endMinute % AppointmentSchedule.SLOT_MINUTES != 0 ->
            R.string.dentists_error_schedule_alignment
        startMinute < 0 || endMinute > MINUTES_PER_DAY ||
            endMinute - startMinute < AppointmentSchedule.SLOT_MINUTES ->
            R.string.dentists_error_schedule_range
        else -> null
    }

    private fun notify(@StringRes messageRes: Int) {
        _events.tryEmit(DentistsEvent.Message(messageRes))
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MINUTES_PER_DAY = 24 * 60

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DentistsViewModel(
                    repository = ServiceLocator.dentistRepository(context),
                    authRepository = ServiceLocator.authRepository,
                    emailValidator = AndroidEmailValidator(),
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
