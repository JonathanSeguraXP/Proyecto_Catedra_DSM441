package com.example.the_white_smile.ui.history

import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.testing.FakeAppointmentRepository
import com.example.the_white_smile.testing.FakeAuthRepository
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
 * Pruebas del [HistoryViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppointmentRepository()
    private val authRepository = FakeAuthRepository().apply { loggedIn = true }

    /** Momento fijo para que el filtro de citas pasadas sea determinista. */
    private val now = 1_000_000L

    private fun createViewModel() = HistoryViewModel(
        repository = repository,
        authRepository = authRepository,
        now = { now }
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: HistoryViewModel): List<HistoryEvent> {
        val events = mutableListOf<HistoryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `excluye las citas pendientes que aun no han ocurrido`() = runTest {
        repository.appointments.value = listOf(
            Appointment("futura", "Limpieza dental", now + 1_000L),
            Appointment("pasada", "Consulta general", now - 1_000L)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("pasada"), viewModel.uiState.value.appointments.map { it.id })
    }

    @Test
    fun `incluye las citas canceladas aunque su fecha sea futura`() = runTest {
        repository.appointments.value = listOf(
            Appointment("cancelada", "Ortodoncia", now + 1_000L, AppointmentStatus.CANCELLED),
            Appointment("pendiente", "Limpieza dental", now + 2_000L)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("cancelada"), viewModel.uiState.value.appointments.map { it.id })
    }

    @Test
    fun `ordena las citas de la mas reciente a la mas antigua`() = runTest {
        repository.appointments.value = listOf(
            Appointment("antigua", "Consulta general", now - 3_000L),
            Appointment("reciente", "Limpieza dental", now - 1_000L),
            Appointment("intermedia", "Blanqueamiento", now - 2_000L)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            listOf("reciente", "intermedia", "antigua"),
            viewModel.uiState.value.appointments.map { it.id }
        )
    }

    @Test
    fun `sin citas queda el estado vacio y sin carga`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.appointments.isEmpty())
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `un error al observar se emite y quita la carga`() = runTest {
        repository.observeError = IllegalStateException("sin permisos")
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is HistoryEvent.Error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
