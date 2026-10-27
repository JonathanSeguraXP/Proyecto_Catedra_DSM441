package com.example.the_white_smile.ui.login

import android.app.Activity
import android.content.Context
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.R
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AndroidConnectivityChecker
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.ConnectivityChecker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logica de la pantalla de login.
 * No conoce Android UI: expone estado y eventos para que la Activity los pinte.
 */
class LoginViewModel(
    private val repository: AuthRepository,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    /** Hay una sesion guardada: la pantalla debe saltar al menu. */
    fun hasActiveSession(): Boolean = repository.isLoggedIn()

    /**
     * Verifica la conexion y avisa al usuario si no hay red.
     * Devuelve false cuando la operacion no debe continuar.
     */
    fun isOnline(): Boolean {
        if (connectivity.isOnline()) return true
        notify(R.string.error_no_connection)
        return false
    }

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return

        val cleanEmail = email.trim()
        when {
            cleanEmail.isEmpty() || password.isEmpty() -> {
                notify(R.string.login_error_empty)
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                notify(R.string.login_error_invalid_email)
                return
            }
            !isOnline() -> return
        }

        runOperation(R.string.loading_login) {
            when (val result = repository.signInWithEmail(cleanEmail, password)) {
                is AuthResult.Success -> _events.emit(LoginEvent.LoggedIn(cleanEmail))
                is AuthResult.Error -> _events.emit(LoginEvent.Error(result.cause))
            }
        }
    }

    fun recoverPassword(email: String) {
        if (_uiState.value.isLoading) return

        val cleanEmail = email.trim()
        when {
            cleanEmail.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                notify(R.string.login_error_invalid_email)
                return
            }
            !isOnline() -> return
        }

        runOperation(R.string.loading_login) {
            when (val result = repository.sendPasswordReset(cleanEmail)) {
                is AuthResult.Success -> notify(R.string.reset_email_sent)
                is AuthResult.Error -> _events.emit(LoginEvent.Error(result.cause))
            }
        }
    }

    /** El flujo de Google ya obtuvo el idToken; falta validarlo con Firebase. */
    fun signInWithGoogle(idToken: String) {
        runOperation(R.string.loading_login) {
            when (val result = repository.signInWithGoogleIdToken(idToken)) {
                is AuthResult.Success -> _events.emit(LoginEvent.LoggedIn(result.value.email.orEmpty()))
                is AuthResult.Error -> _events.emit(LoginEvent.Error(result.cause))
            }
        }
    }

    fun signInWithGithub(activity: Activity) {
        runOperation(R.string.loading_login) {
            when (val result = repository.signInWithGithub(activity)) {
                is AuthResult.Success -> _events.emit(LoginEvent.LoggedIn(result.value.email.orEmpty()))
                is AuthResult.Error -> _events.emit(LoginEvent.Error(result.cause))
            }
        }
    }

    /** Un flujo externo (selector de cuentas, navegador) tomo el control. */
    fun onExternalFlowStarted() {
        _uiState.update {
            it.copy(isLoading = true, loadingMessage = R.string.loading_login)
        }
    }

    /** El flujo externo termino sin resultado util: se libera la pantalla. */
    fun onExternalFlowCancelled() {
        _uiState.update { it.copy(isLoading = false) }
    }

    /** La Activity detecto un error fuera del repositorio y quiere mostrarlo igual. */
    fun reportError(cause: Throwable?) {
        _events.tryEmit(LoginEvent.Error(cause))
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
        _events.tryEmit(LoginEvent.Message(messageRes))
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LoginViewModel(
                    repository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext)
                )
            }
        }
    }
}
