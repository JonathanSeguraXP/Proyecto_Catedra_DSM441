package com.example.the_white_smile.data

import com.example.the_white_smile.domain.Dentist
import kotlinx.coroutines.flow.Flow

/** Contrato de administracion de los dentistas del consultorio. */
interface DentistRepository {

    /** Lista de dentistas en tiempo real, ordenada por nombre. */
    fun observeDentists(): Flow<List<Dentist>>

    /**
     * Crea la cuenta del dentista y su perfil en Firestore.
     * La cuenta se crea en una instancia secundaria de Firebase para no
     * cerrar la sesion del administrador.
     */
    suspend fun createDentist(
        displayName: String,
        email: String,
        password: String,
        specialty: String,
        workDays: Set<Int> = Dentist.DEFAULT_WORK_DAYS,
        startMinute: Int = Dentist.DEFAULT_START_MINUTE,
        endMinute: Int = Dentist.DEFAULT_END_MINUTE
    ): Result<Dentist>

    /** Activa o desactiva un dentista sin eliminarlo. */
    suspend fun setActive(dentistId: String, active: Boolean): Result<Unit>

    /** Actualiza los dias y el rango de atencion del dentista. */
    suspend fun updateSchedule(
        dentistId: String,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Result<Unit>

    /** Guarda una miniatura comprimida en el perfil publico del dentista. */
    suspend fun updateProfilePhoto(dentistId: String, photoBase64: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("No se puede actualizar la foto"))
}
