package com.example.the_white_smile.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.UserRepository
import com.example.the_white_smile.domain.UserRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Logica de la pantalla del menu principal.
 * Expone los datos de la sesion, el rol del usuario y el cierre de sesion.
 */
class MenuViewModel(
    private val repository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MenuUiState(displayName = repository.currentUser?.displayName?.trim()?.ifBlank { null })
    )
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MenuEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<MenuEvent> = _events.asSharedFlow()

    private var roleJob: Job? = null

    init {
        refresh()
    }

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = repository.isLoggedIn()

    /**
     * Vuelve a leer el nombre y el rol, por si se edito el perfil o el
     * administrador cambio los permisos de la cuenta.
     */
    fun refresh() {
        val user = repository.currentUser
        _uiState.update {
            it.copy(displayName = user?.displayName?.trim()?.ifBlank { null })
        }

        if (user == null) return

        roleJob?.cancel()
        roleJob = viewModelScope.launch {
            val role = userRepository.getProfile(user.uid).getOrNull()?.role
                ?: UserRole.PATIENT
            _uiState.update { it.copy(role = role) }
        }
    }

    fun logout() {
        repository.signOut()
        _events.tryEmit(MenuEvent.LoggedOut)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MenuViewModel(
                    repository = ServiceLocator.authRepository,
                    userRepository = ServiceLocator.userRepository
                )
            }
        }
    }
}
