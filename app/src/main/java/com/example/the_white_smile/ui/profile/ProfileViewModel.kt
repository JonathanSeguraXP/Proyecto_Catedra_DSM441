package com.example.the_white_smile.ui.profile

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
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.UserRepository
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
 * Logica de la pantalla de perfil.
 * Lee y actualiza los datos de la cuenta a traves del repositorio.
 */
class ProfileViewModel(
    private val repository: AuthRepository,
    private val connectivity: ConnectivityChecker,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadSession()
        loadSpecialty()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = repository.isLoggedIn()

    /**
     * Vuelve a leer la sesion desde el servidor. Se llama al volver a la
     * pantalla, por ejemplo despues de verificar el correo.
     */
    fun refresh() {
        viewModelScope.launch {
            repository.reloadUser()
            loadSession()
            loadSpecialty()
        }
    }

    fun saveName(name: String) {
        if (_uiState.value.isLoading) return

        val cleanName = name.trim()
        when {
            cleanName.isEmpty() -> {
                notify(R.string.profile_error_name_empty)
                return
            }
            cleanName.length > MAX_NAME_LENGTH -> {
                notify(R.string.profile_error_name_too_long)
                return
            }
            cleanName == _uiState.value.name -> {
                notify(R.string.profile_name_unchanged)
                return
            }
            !isOnline() -> return
        }

        runOperation(R.string.loading_profile) {
            when (val result = repository.updateDisplayName(cleanName)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(name = result.value.displayName ?: cleanName)
                    }
                    notify(R.string.profile_name_updated)
                }
                is AuthResult.Error -> _events.emit(ProfileEvent.Error(result.cause))
            }
        }
    }

    fun sendVerificationEmail() {
        if (_uiState.value.isLoading) return
        if (_uiState.value.isEmailVerified) {
            notify(R.string.profile_email_already_verified)
            return
        }
        if (!isOnline()) return

        runOperation(R.string.loading_verification) {
            when (val result = repository.sendEmailVerification()) {
                is AuthResult.Success -> notify(R.string.profile_verification_sent)
                is AuthResult.Error -> _events.emit(ProfileEvent.Error(result.cause))
            }
        }
    }

    fun sendPasswordReset() {
        if (_uiState.value.isLoading) return

        val email = _uiState.value.email?.trim().orEmpty()
        if (email.isEmpty()) {
            notify(R.string.error_unknown)
            return
        }
        if (!isOnline()) return

        runOperation(R.string.loading_password) {
            when (val result = repository.sendPasswordReset(email)) {
                is AuthResult.Success -> notify(R.string.reset_email_sent)
                is AuthResult.Error -> _events.emit(ProfileEvent.Error(result.cause))
            }
        }
    }

    private fun loadSession() {
        val user = repository.currentUser ?: return
        _uiState.update {
            it.copy(
                name = user.displayName.orEmpty(),
                email = user.email,
                isEmailVerified = repository.isEmailVerified()
            )
        }
    }

    private fun loadSpecialty() {
        val uid = repository.currentUser?.uid ?: return
        val profiles = userRepository ?: return
        viewModelScope.launch {
            val specialty = profiles.getProfile(uid).getOrNull()?.specialty
            _uiState.update { it.copy(specialty = specialty?.takeIf(String::isNotBlank)) }
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
        _events.tryEmit(ProfileEvent.Message(messageRes))
    }

    companion object {
        /** Firebase no impone un limite, pero la interfaz se mantiene razonable. */
        private const val MAX_NAME_LENGTH = 40

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    repository = ServiceLocator.authRepository,
                    connectivity = AndroidConnectivityChecker(context.applicationContext),
                    userRepository = ServiceLocator.userRepository
                )
            }
        }
    }
}
