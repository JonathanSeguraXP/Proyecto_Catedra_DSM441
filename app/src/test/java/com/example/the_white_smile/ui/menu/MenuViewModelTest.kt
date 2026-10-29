package com.example.the_white_smile.ui.menu

import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.UserProfile
import com.example.the_white_smile.domain.UserRole
import com.example.the_white_smile.testing.FakeAuthRepository
import com.example.the_white_smile.testing.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas del [MenuViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository().apply { loggedIn = true }
    private val userRepository = FakeUserRepository()

    private fun createViewModel() = MenuViewModel(
        repository = repository,
        userRepository = userRepository
    )

    private fun profile(role: UserRole) = UserProfile(
        uid = "uid-1",
        email = "ana@correo.com",
        displayName = "Ana",
        role = role
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: MenuViewModel): List<MenuEvent> {
        val events = mutableListOf<MenuEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `al iniciar muestra el nombre de la sesion`() {
        val viewModel = createViewModel()

        assertEquals("Ana", viewModel.uiState.value.displayName)
        assertTrue(viewModel.hasActiveSession())
    }

    @Test
    fun `sin perfil el usuario se trata como paciente`() = runTest {
        userRepository.profile = null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UserRole.PATIENT, viewModel.uiState.value.role)
        assertEquals(1, userRepository.calls)
    }

    @Test
    fun `un perfil con rol admin se refleja en el estado`() = runTest {
        userRepository.profile = profile(UserRole.ADMIN)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UserRole.ADMIN, viewModel.uiState.value.role)
    }

    @Test
    fun `un perfil con rol dentista se refleja en el estado`() = runTest {
        userRepository.profile = profile(UserRole.DENTIST)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UserRole.DENTIST, viewModel.uiState.value.role)
    }

    @Test
    fun `si falla la lectura del perfil se asume paciente`() = runTest {
        userRepository.error = IllegalStateException("sin conexion")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UserRole.PATIENT, viewModel.uiState.value.role)
    }

    @Test
    fun `un nombre en blanco no se usa como saludo`() = runTest {
        repository.user = repository.user.copy(displayName = "   ")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.displayName)
    }

    @Test
    fun `refresh vuelve a leer el nombre y el rol`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(UserRole.PATIENT, viewModel.uiState.value.role)

        repository.user = repository.user.copy(displayName = "Ana Maria")
        userRepository.profile = profile(UserRole.DENTIST)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("Ana Maria", viewModel.uiState.value.displayName)
        assertEquals(UserRole.DENTIST, viewModel.uiState.value.role)
        assertEquals(2, userRepository.calls)
    }

    @Test
    fun `sin sesion no consulta el perfil y avisa que no hay sesion`() = runTest {
        repository.loggedIn = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.displayName)
        assertNull(viewModel.uiState.value.role)
        assertEquals(0, userRepository.calls)
        assertFalse(viewModel.hasActiveSession())
    }

    @Test
    fun `cerrar sesion emite el evento y quita la sesion`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.logout()
        advanceUntilIdle()

        assertEquals(listOf(MenuEvent.LoggedOut), events)
        assertFalse(repository.isLoggedIn())
    }
}
