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

    suspend fun createAppointment(
        service: String,
        startAt: Long,
        dentistId: String,
        dentistName: String
    ): Result<Unit>

    suspend fun cancelAppointment(id: String): Result<Unit>
}
