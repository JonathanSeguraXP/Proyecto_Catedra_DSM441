package com.example.the_white_smile.ui.agenda

import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.SessionUser
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
 * Pruebas del [DentistAgendaViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DentistAgendaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppointmentRepository()
    private val authRepository = FakeAuthRepository().apply {
        loggedIn = true
        user = SessionUser(
            uid = "dentist-1",
            email = "ana@correo.com",
            displayName = "Dra. Ana"
        )
    }
    private val connectivity = FakeConnectivityChecker()

    /** Momento fijo para que las validaciones de fecha sean deterministas. */
    private val now = 1_000_000L

    private fun createViewModel() = DentistAgendaViewModel(
        repository = repository,
        authRepository = authRepository,
        connectivity = connectivity,
        now = { now }
    )

    private fun appointment(
        id: String = "a1",
        startAt: Long = now - 60_000L,
        status: AppointmentStatus = AppointmentStatus.PENDING,
        patientId: String? = "patient-1"
    ) = Appointment(
        id = id,
        service = "Limpieza dental",
        startAt = startAt,
        status = status,
        dentistId = "dentist-1",
        dentistName = "Dra. Ana",
        durationMinutes = 30,
        patientId = patientId,
        patientName = "Ana Torres"
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(
        viewModel: DentistAgendaViewModel
    ): List<DentistAgendaEvent> {
        val events = mutableListOf<DentistAgendaEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `observa las citas del dentista ordenadas por fecha`() = runTest {
        repository.dentistAppointments.value = listOf(
            appointment(id = "futura", startAt = now + 60_000L),
            appointment(id = "pasada", startAt = now - 60_000L)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("pasada", "futura"), viewModel.uiState.value.appointments.map { it.id })
        assertEquals(1, repository.dentistObserveCalls)
        assertEquals("dentist-1", repository.lastObservedDentistId)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `sin citas asignadas queda el estado vacio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.appointments.isEmpty())
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sin sesion activa no permite entrar a la pantalla`() = runTest {
        authRepository.loggedIn = false
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.hasActiveSession())
        assertTrue(viewModel.uiState.value.appointments.isEmpty())
    }

    @Test
    fun `completa una cita pendiente cuya hora ya paso`() = runTest {
        val cita = appointment()
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertEquals(listOf(DentistAgendaEvent.Message(R.string.agenda_completed)), events)
        assertEquals(1, repository.completeCalls)
        assertEquals(cita, repository.lastCompletedAppointment)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `no completa una cita futura`() = runTest {
        val cita = appointment(startAt = now + 60_000L)
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertEquals(listOf(DentistAgendaEvent.Message(R.string.agenda_error_future)), events)
        assertEquals(0, repository.completeCalls)
    }

    @Test
    fun `una cita ya completada no se vuelve a completar`() = runTest {
        val cita = appointment(status = AppointmentStatus.COMPLETED)
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertEquals(0, repository.completeCalls)
    }

    @Test
    fun `una cita antigua sin paciente no se puede completar`() = runTest {
        val cita = appointment(patientId = null)
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertEquals(listOf(DentistAgendaEvent.Message(R.string.agenda_error_legacy)), events)
        assertEquals(0, repository.completeCalls)
    }

    @Test
    fun `sin conexion avisa y no llama al repositorio`() = runTest {
        connectivity.online = false
        val cita = appointment()
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertEquals(listOf(DentistAgendaEvent.Message(R.string.error_no_connection)), events)
        assertEquals(0, repository.completeCalls)
    }

    @Test
    fun `un error al completar se emite como evento de error`() = runTest {
        repository.completeResult = Result.failure(IllegalStateException("sin permisos"))
        val cita = appointment()
        repository.dentistAppointments.value = listOf(cita)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.completeAppointment(cita)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is DentistAgendaEvent.Error)
        assertEquals(1, repository.completeCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `un error al observar la agenda se emite y quita la carga`() = runTest {
        repository.dentistObserveError = IllegalStateException("sin permisos")
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is DentistAgendaEvent.Error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
