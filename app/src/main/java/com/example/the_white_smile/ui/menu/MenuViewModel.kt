package com.example.the_white_smile.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.the_white_smile.core.ServiceLocator
import com.example.the_white_smile.data.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Logica de la pantalla del menu principal.
 * Expone los datos de la sesion y el cierre de sesion.
 */
class MenuViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MenuUiState(displayName = repository.currentUser?.displayName?.trim()?.ifBlank { null })
    )
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MenuEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<MenuEvent> = _events.asSharedFlow()

    /** La pantalla solo debe mostrarse con una sesion activa. */
    fun hasActiveSession(): Boolean = repository.isLoggedIn()

    fun logout() {
        repository.signOut()
        _events.tryEmit(MenuEvent.LoggedOut)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MenuViewModel(repository = ServiceLocator.authRepository)
            }
        }
    }
}
