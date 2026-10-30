package com.example.the_white_smile.ui.appointments

import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentSchedule
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.domain.SlotConflictException
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.testing.FakeAppointmentRepository
import com.example.the_white_smile.testing.FakeAuthRepository
import com.example.the_white_smile.testing.FakeConnectivityChecker
import com.example.the_white_smile.testing.FakeDentistRepository
import com.example.the_white_smile.testing.FakeTreatmentRepository
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
import java.util.Calendar

/**
 * Pruebas del [AppointmentsViewModel] con repositorios falsos:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAppointmentRepository()
    private val dentistRepository = FakeDentistRepository().apply {
        dentists.value = listOf(
            Dentist(
                id = "d1",
                displayName = "Dra. Ana Ramirez",
                email = "ana@correo.com",
                specialty = "Ortodoncia",
                // Todos los dias para que la prueba no dependa del dia actual.
                workDays = setOf(1, 2, 3, 4, 5, 6, 7)
            )
        )
    }
    private val treatmentRepository = FakeTreatmentRepository(
        listOf(
            Treatment(
                id = "limpieza",
                name = "Limpieza dental",
                summary = "Limpieza",
                description = "Limpieza dental",
                durationMinutes = 30,
                recommendations = emptyList()
            )
        )
    )
    private val authRepository = FakeAuthRepository().apply { loggedIn = true }
    private val connectivity = FakeConnectivityChecker()

    /** Manana a las 10:00, alineado a un bloque de 15 minutos. */
    private val futureStartAt: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val dentistId = "d1"
    private val treatmentId = "limpieza"

    private fun createViewModel() = AppointmentsViewModel(
        repository = repository,
        dentistRepository = dentistRepository,
        treatmentRepository = treatmentRepository,
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
    fun `carga el catalogo de tratamientos`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.treatments.size)
        assertEquals("limpieza", viewModel.uiState.value.treatments.first().id)
    }

    @Test
    fun `sin sesion activa no permite entrar a la pantalla`() {
        authRepository.loggedIn = false
        val viewModel = createViewModel()

        assertFalse(viewModel.hasActiveSession())
    }

    @Test
    fun `un tratamiento vacio avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("", futureStartAt, dentistId)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_treatment)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un tratamiento inexistente avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment("no-existe", futureStartAt, dentistId)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_treatment)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `una fecha pasada avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(
            treatmentId,
            System.currentTimeMillis() - 1_000L,
            dentistId
        )
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_past)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un horario fuera de atencion avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        val lateStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModel.createAppointment(treatmentId, lateStart, dentistId)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_schedule)),
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

        viewModel.createAppointment(treatmentId, futureStartAt, dentistId)
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

        viewModel.createAppointment(treatmentId, futureStartAt, dentistId)
        advanceUntilIdle()

        assertEquals(listOf(AppointmentsEvent.Message(R.string.appointment_created)), events)
        assertEquals(1, repository.createCalls)
        assertEquals("Limpieza dental", repository.lastService)
        assertEquals(treatmentId, repository.lastTreatmentId)
        assertEquals(30, repository.lastDurationMinutes)
        assertEquals(futureStartAt, repository.lastStartAt)
        assertEquals(dentistId, repository.lastDentistId)
        assertEquals("Dra. Ana Ramirez", repository.lastDentistName)
        assertEquals("Ana", repository.lastPatientName)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `si otro paciente ocupa el horario avisa al confirmar`() = runTest {
        repository.createResult = Result.failure(SlotConflictException())
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(treatmentId, futureStartAt, dentistId)
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_slot_taken)),
            events
        )
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun `sin conexion avisa y no llama al repositorio`() = runTest {
        connectivity.online = false
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(treatmentId, futureStartAt, dentistId)
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
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(treatmentId, futureStartAt, dentistId)
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
        assertEquals(appointment, repository.lastCancelledAppointment)
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

    @Test
    fun `solo se ofrecen los dentistas activos`() = runTest {
        dentistRepository.dentists.value = dentistRepository.dentists.value + Dentist(
            id = "d2",
            displayName = "Dr. Inactivo",
            email = "inactivo@correo.com",
            specialty = "General",
            active = false
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("d1"), viewModel.uiState.value.dentists.map { it.id })
    }

    @Test
    fun `sin dentista seleccionado avisa y no guarda la cita`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(treatmentId, futureStartAt, "")
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_dentist)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `un dentista inexistente o inactivo avisa y no guarda la cita`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.createAppointment(treatmentId, futureStartAt, "no-existe")
        advanceUntilIdle()

        assertEquals(
            listOf(AppointmentsEvent.Message(R.string.appointments_error_dentist)),
            events
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `si falla la lista de dentistas se emite el error`() = runTest {
        dentistRepository.observeError = IllegalStateException("sin permisos")
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is AppointmentsEvent.Error)
    }

    @Test
    fun `la disponibilidad excluye las horas ocupadas`() = runTest {
        val dayStart = AppointmentSchedule.startOfDay(futureStartAt)
        // Ocupa solo el bloque de las 10:15; el turno de 30 min de las 10:00 se cruza.
        repository.busySlots.value = setOf(
            AppointmentSchedule.slotIndex(futureStartAt + 15 * 60_000L)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestAvailability(dentistId, treatmentId, dayStart)
        advanceUntilIdle()

        val availability = viewModel.uiState.value.availability
            ?: error("No se cargo la disponibilidad")
        assertFalse(availability.isLoading)
        assertFalse(availability.times.contains(futureStartAt))
        assertTrue(availability.times.contains(futureStartAt - 30 * 60_000L))
        assertEquals(dentistId, repository.lastBusyDentistId)
        assertEquals(dayStart, repository.lastBusyDayStart)
        assertEquals(AppointmentSchedule.endOfDay(dayStart), repository.lastBusyDayEnd)
    }

    @Test
    fun `sin horas libres la disponibilidad queda vacia`() = runTest {
        val dayStart = AppointmentSchedule.startOfDay(futureStartAt)
        // El horario de 08:00 a 17:00 se llena por completo.
        val allSlots = (0 until (9 * 60 / AppointmentSchedule.SLOT_MINUTES))
            .map { index ->
                AppointmentSchedule.slotIndex(
                    AppointmentSchedule.atMinute(dayStart, 8 * 60) +
                        index * AppointmentSchedule.SLOT_MILLIS
                )
            }
            .toSet()
        repository.busySlots.value = allSlots
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestAvailability(dentistId, treatmentId, dayStart)
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), viewModel.uiState.value.availability?.times)
    }

    @Test
    fun `si falla la consulta de disponibilidad se emite el error`() = runTest {
        repository.busyError = IllegalStateException("sin permisos")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val events = collectEvents(viewModel)

        viewModel.requestAvailability(
            dentistId,
            treatmentId,
            AppointmentSchedule.startOfDay(futureStartAt)
        )
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.first() is AppointmentsEvent.Error)
    }

    @Test
    fun `cerrar el dialogo limpia la disponibilidad`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestAvailability(
            dentistId,
            treatmentId,
            AppointmentSchedule.startOfDay(futureStartAt)
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.availability != null)

        viewModel.clearAvailability()
        assertNull(viewModel.uiState.value.availability)
    }
}
