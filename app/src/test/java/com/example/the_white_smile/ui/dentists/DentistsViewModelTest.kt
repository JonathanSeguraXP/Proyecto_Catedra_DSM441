package com.example.the_white_smile.ui.dentists

import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.testing.FakeAuthRepository
import com.example.the_white_smile.testing.FakeConnectivityChecker
import com.example.the_white_smile.testing.FakeDentistRepository
import com.example.the_white_smile.testing.FakeEmailValidator
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
 * Pruebas del [DentistsViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DentistsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDentistRepository()
    private val authRepository = FakeAuthRepository().apply { loggedIn = true }
    private val connectivity = FakeConnectivityChecker()
    private val emailValidator = FakeEmailValidator()

    private fun createViewModel() = DentistsViewModel(
        repository = repository,
        authRepository = authRepository,
        emailValidator = emailValidator,
        connectivity = connectivity
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: DentistsViewModel): List<DentistsEvent> {
        val events = mutableListOf<DentistsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private fun dentist(id: String = "d1", active: Boolean = true) = Dentist(
        id = id,
        displayName = "Dra. Ana Ramirez",
        email = "ana@correo.com",
        specialty = "Ortodoncia",
        active = active
    )

    @Test
    fun `observa los dentistas del repositorio`() = runTest {
        repository.dentists.value = listOf(dentist())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.dentists.size)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `sin dentistas muestra el estado vacio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `sin sesion activa no permite entrar a la pantalla`() {
        authRepository.loggedIn = false
        val viewModel = createViewModel()

        assertFalse(viewModel.hasActiveSession())
    }

    @Test
    fun `un nombre vacio avisa y no crea la cuenta`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("   ", "Ortodoncia", "ana@correo.com", "secreto1")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_error_name)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una especialidad vacia avisa y no crea la cuenta`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "  ", "ana@correo.com", "secreto1")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_error_specialty)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un correo vacio avisa y no crea la cuenta`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "Ortodoncia", "  ", "secreto1")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_error_email)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un correo invalido avisa y no crea la cuenta`() = runTest {
        emailValidator.valid = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "Ortodoncia", "ana", "secreto1")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_error_email)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una contrasena corta avisa y no crea la cuenta`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "Ortodoncia", "ana@correo.com", "12345")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_error_password)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `sin conexion avisa y no crea la cuenta`() = runTest {
        connectivity.online = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "Ortodoncia", "ana@correo.com", "secreto1")
        advanceUntilIdle()

        assertEquals(
            listOf(DentistsEvent.Message(R.string.error_no_connection)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una cuenta valida se crea y emite el aviso`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist(
            "  Dra. Ana  ",
            "  Ortodoncia ",
            " ana@correo.com ",
            "secreto1"
        )
        advanceUntilIdle()

        assertEquals(1, repository.createCalls)
        assertEquals("Dra. Ana", repository.lastName)
        assertEquals("ana@correo.com", repository.lastEmail)
        assertEquals("Ortodoncia", repository.lastSpecialty)
        assertEquals(listOf(DentistsEvent.Created("Dra. Ana")), events)
        assertFalse(viewModel.uiState.value.isCreating)
    }

    @Test
    fun `si falla la creacion emite el error`() = runTest {
        repository.createResult = Result.failure(IllegalStateException("correo en uso"))
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createDentist("Dra. Ana", "Ortodoncia", "ana@correo.com", "secreto1")
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue((events.single() as DentistsEvent.Error).cause is IllegalStateException)
        assertFalse(viewModel.uiState.value.isCreating)
    }

    @Test
    fun `activar o desactivar actualiza el dentista`() = runTest {
        repository.dentists.value = listOf(dentist(active = true))
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.setActive(dentist(active = true), false)
        advanceUntilIdle()

        assertEquals(1, repository.setActiveCalls)
        assertEquals("d1", repository.lastActiveId)
        assertEquals(false, repository.lastActiveValue)
        assertEquals(
            listOf(DentistsEvent.Message(R.string.dentists_deactivated)),
            events
        )
    }

    @Test
    fun `cambiar al mismo estado no llama al repositorio`() = runTest {
        repository.dentists.value = listOf(dentist(active = true))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setActive(dentist(active = true), true)
        advanceUntilIdle()

        assertEquals(0, repository.setActiveCalls)
    }

    @Test
    fun `si falla el cambio de estado emite el error`() = runTest {
        repository.dentists.value = listOf(dentist(active = true))
        repository.setActiveResult = Result.failure(IllegalStateException("sin permisos"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.setActive(dentist(active = true), false)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.single() is DentistsEvent.Error)
    }

    @Test
    fun `si falla la escucha de dentistas emite el error`() = runTest {
        repository.observeError = IllegalStateException("sin conexion")
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.single() is DentistsEvent.Error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
