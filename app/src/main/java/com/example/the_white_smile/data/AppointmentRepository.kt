package com.example.the_white_smile.data

import com.example.the_white_smile.domain.Appointment
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a las citas del usuario.
 * La implementacion actual usa Firestore; otra implementacion podria
 * usar una base local sin cambiar los ViewModel.
 */
interface AppointmentRepository {

    /** Emite la lista de citas del usuario cada vez que cambia en el servidor. */
    fun observeAppointments(): Flow<List<Appointment>>

    /**
     * Emite las citas asignadas al dentista, sin importar el paciente.
     * Se usa en la agenda del dentista y requiere una regla de grupo de
     * colecciones en Firestore.
     */
    fun observeDentistAppointments(dentistId: String): Flow<List<Appointment>>

    /**
     * Emite los bloques ocupados de un dentista en un dia.
     * Solo expone la ocupacion, no las citas de otros pacientes.
     */
    fun observeBusySlots(
        dentistId: String,
        dayStartAt: Long,
        dayEndAt: Long
    ): Flow<Set<Long>>

    /**
     * Reserva la cita y todos los bloques que ocupa.
     * Si algun bloque ya esta ocupado falla con
     * [com.example.the_white_smile.domain.SlotConflictException].
     */
    suspend fun createAppointment(
        service: String,
        startAt: Long,
        treatmentId: String,
        durationMinutes: Int,
        dentistId: String,
        dentistName: String,
        patientName: String? = null
    ): Result<Unit>

    /** Reserva la cita y guarda el precio total y el abono acordado. */
    suspend fun createAppointment(
        service: String,
        startAt: Long,
        treatmentId: String,
        durationMinutes: Int,
        dentistId: String,
        dentistName: String,
        patientName: String?,
        totalPriceCents: Long,
        depositCents: Long,
        depositPercent: Int
    ): Result<Unit> = createAppointment(
        service = service,
        startAt = startAt,
        treatmentId = treatmentId,
        durationMinutes = durationMinutes,
        dentistId = dentistId,
        dentistName = dentistName,
        patientName = patientName
    )

    /** Cancela la cita y libera sus bloques de la agenda del dentista. */
    suspend fun cancelAppointment(appointment: Appointment): Result<Unit>

    /**
     * Marca como completada una cita que ya ocurrio.
     * Solo el dentista asignado puede hacerlo y la cita debe tener paciente.
     */
    suspend fun completeAppointment(appointment: Appointment): Result<Unit>
}
