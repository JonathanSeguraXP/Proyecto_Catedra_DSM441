package com.example.the_white_smile.ui.appointments

import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.testing.FakeAppointmentRepository
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
 * Pruebas del [AppointmentsViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppointmentRepository()
    private val authRepository = FakeAuthRepository().apply { loggedIn = true }
    private val connectivity = FakeConnectivityChecker()

    /** Momento futuro usado como fecha valida en las pruebas. */
    private val futureStartAt = System.currentTimeMillis() + 3_600_000L

    private fun createViewModel() = AppointmentsViewModel(
        repository = repository,
        authRepository = authRepository,
        connectivity = connectivity
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: AppointmentsViewModel): List<AppointmentsEvent> {
        val events = mutableListOf<AppointmentsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `observa las citas del repositorio`() = runTest {
        repository.appointments.value = listOf(
            Appointment("a1", "Limpieza dental", futureStartAt)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.appointments.size)
        assertFalse(viewModel.uiState.value.isEmpty)
        assertEquals("Limpieza dental", viewModel.uiState.value.appointments.first().service)
    }

    @Test
    fun `sin sesion activa no permite entrar a la pantalla`() {
        authRepository.loggedIn = false
        val viewModel = createViewModel()

        assertFalse(viewModel.hasActiveSession())
    }

    @Test
    fun `un servicio vacio avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("   ", futureStartAt)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_service)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una fecha pasada avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("Limpieza dental", System.currentTimeMillis() - 1_000L)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_past)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un horario ocupado avisa y no llama al repositorio`() = runTest {
        repository.appointments.value = listOf(
            Appointment("a1", "Consulta general", futureStartAt, AppointmentStatus.PENDING)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("Limpieza dental", futureStartAt)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_conflict)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una cita cancelada no bloquea el horario`() = runTest {
        repository.appointments.value = listOf(
            Appointment("a1", "Consulta general", futureStartAt, AppointmentStatus.CANCELLED)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("Limpieza dental", futureStartAt)
        advanceUntilIdle()

        assertEquals(listOf(AppointmentsEvent.Message(R.string.appointment_created)), events)
        assertEquals(1, repository.createCalls)
        assertEquals("Limpieza dental", repository.lastService)
        assertEquals(futureStartAt, repository.lastStartAt)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sin conexion avisa y no llama al repositorio`() = runTest {
        connectivity.online = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("Limpieza dental", futureStartAt)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.error_no_connection)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un error al guardar se emite como evento de error`() = runTest {
        repository.createResult = Result.failure(IllegalStateException("fallo"))
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("Limpieza dental", futureStartAt)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is AppointmentsEvent.Error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `cancelar una cita pendiente actualiza el repositorio`() = runTest {
        val appointment = Appointment("a1", "Limpieza dental", futureStartAt)
        repository.appointments.value = listOf(appointment)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.cancelAppointment(appointment)
        advanceUntilIdle()

        assertEquals(listOf(AppointmentsEvent.Message(R.string.appointment_cancelled)), events)
        assertEquals(1, repository.cancelCalls)
        assertEquals("a1", repository.lastCancelledId)
    }

    @Test
    fun `una cita ya cancelada no vuelve a cancelarse`() = runTest {
        val appointment = Appointment(
            "a1",
            "Limpieza dental",
            futureStartAt,
            AppointmentStatus.CANCELLED
        )
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.cancelAppointment(appointment)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertEquals(0, repository.cancelCalls)
    }

    @Test
    fun `un error al observar las citas se emite como evento`() = runTest {
        repository.observeError = IllegalStateException("sin permisos")
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is AppointmentsEvent.Error)
    }
}
