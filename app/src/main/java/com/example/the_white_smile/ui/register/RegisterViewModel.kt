package com.example.the_white_smile.ui.register

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
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.EmailValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logica de la pantalla de registro.
 * Valida los campos y crea la cuenta a traves del repositorio.
 */
class RegisterViewModel(
    private val repository: AuthRepository,
    private val connectivity: ConnectivityChecker,
    private val emailValidator: EmailValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RegisterEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<RegisterEvent> = _events.asSharedFlow()

    fun register(name: String, email: String, password: String, confirm: String) {
        if (_uiState.value.isLoading) return

        val cleanName = name.trim()
        val cleanEmail = email.trim()

        when {
            cleanName.isEmpty() -> {
                notify(R.string.register_error_name_empty)
                return
            }
            cleanEmail.isEmpty() || password.isEmpty() || confirm.isEmpty() -> {
                notify(R.string.register_error_empty)
                return
            }
            !emailValidator.isValid(cleanEmail) -> {
                notify(R.string.login_error_invalid_email)
                return
            }
            password != confirm -> {
                notify(R.string.register_password_mismatch)
                return
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                notify(R.string.register_password_short)
                return
            }
            !isOnline() -> return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = R.string.loading_register)
            }
            try {
                val result = repository.registerWithEmail(cleanName, cleanEmail, password)
                when (result) {
                    is AuthResult.Success -> _events.emit(RegisterEvent.Registered)
                    is AuthResult.Error -> _events.emit(RegisterEvent.Error(result.cause))
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun isOnline(): Boolean {
        if (connectivity.isOnline()) return true
        notify(R.string.error_no_connection)
        return false
    }

    private fun notify(@StringRes messageRes: Int) {
        _events.tryEmit(RegisterEvent.Message(messageRes))
    }

    companion object {
        /** Firebase exige un minimo de 6 caracteres para la contrasena. */
        private const val MIN_PASSWORD_LENGTH = 6

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RegisterViewModel(
                    repository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext),
                    emailValidator = AndroidEmailValidator()
                )
            }
        }
    }
}
