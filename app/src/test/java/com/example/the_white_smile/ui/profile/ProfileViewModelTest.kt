package com.example.the_white_smile.ui.profile

import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.testing.FakeAuthRepository
import com.example.the_white_smile.testing.FakeConnectivityChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas del [ProfileViewModel] con un repositorio falso:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository().apply { loggedIn = true }
    private val connectivity = FakeConnectivityChecker()

    private fun createViewModel() = ProfileViewModel(
        repository = repository,
        connectivity = connectivity
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: ProfileViewModel): List<ProfileEvent> {
        val events = mutableListOf<ProfileEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `al iniciar carga los datos de la sesion`() {
        val viewModel = createViewModel()

        assertEquals("Ana", viewModel.uiState.value.name)
        assertEquals("ana@correo.com", viewModel.uiState.value.email)
        assertFalse(viewModel.uiState.value.isEmailVerified)
        assertTrue(viewModel.hasActiveSession())
    }

    @Test
    fun `guardar un nombre vacio avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.saveName("   ")
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.profile_error_name_empty)), events)
        assertEquals(0, repository.updateNameCalls)
    }

    @Test
    fun `guardar el mismo nombre avisa sin llamar al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.saveName("Ana")
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.profile_name_unchanged)), events)
        assertEquals(0, repository.updateNameCalls)
    }

    @Test
    fun `un nombre demasiado largo avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.saveName("a".repeat(41))
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.profile_error_name_too_long)), events)
        assertEquals(0, repository.updateNameCalls)
    }

    @Test
    fun `guardar un nombre valido actualiza el estado`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.saveName("  Ana Perez  ")
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.profile_name_updated)), events)
        assertEquals("Ana Perez", viewModel.uiState.value.name)
        assertEquals("Ana Perez", repository.lastUpdatedName)
        assertEquals(1, repository.updateNameCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sin conexion avisa y no llama al repositorio`() = runTest {
        connectivity.online = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.saveName("Otra Persona")
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.error_no_connection)), events)
        assertEquals(0, repository.updateNameCalls)
    }

    @Test
    fun `enviar la verificacion avisa al usuario`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.sendVerificationEmail()
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.profile_verification_sent)), events)
        assertEquals(1, repository.verificationCalls)
    }

    @Test
    fun `si el correo ya esta verificado no se envia otro correo`() = runTest {
        repository.emailVerified = true
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.sendVerificationEmail()
        advanceUntilIdle()

        assertEquals(
            listOf(ProfileEvent.Message(R.string.profile_email_already_verified)),
            events
        )
        assertEquals(0, repository.verificationCalls)
    }

    @Test
    fun `refresh actualiza el estado de verificacion`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isEmailVerified)

        repository.emailVerified = true
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmailVerified)
        assertEquals(1, repository.reloadCalls)
    }

    @Test
    fun `cambiar la contrasena envia el correo de la cuenta`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.sendPasswordReset()
        advanceUntilIdle()

        assertEquals(listOf(ProfileEvent.Message(R.string.reset_email_sent)), events)
        assertEquals("ana@correo.com", repository.lastResetEmail)
        assertEquals(1, repository.passwordResetCalls)
    }
}
