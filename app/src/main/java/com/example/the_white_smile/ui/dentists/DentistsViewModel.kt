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
        password: String
    ) {
        if (_uiState.value.isCreating) return

        val cleanName = displayName.trim()
        val cleanSpecialty = specialty.trim()
        val cleanEmail = email.trim()

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
            !isOnline() -> return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                repository.createDentist(
                    displayName = cleanName,
                    email = cleanEmail,
                    password = password,
                    specialty = cleanSpecialty
                )
                    .onSuccess { _events.emit(DentistsEvent.Created(it.displayName)) }
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

    private fun notify(@StringRes messageRes: Int) {
        _events.tryEmit(DentistsEvent.Message(messageRes))
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6

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
