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
        specialty: String
    ): Result<Dentist>

    /** Activa o desactiva un dentista sin eliminarlo. */
    suspend fun setActive(dentistId: String, active: Boolean): Result<Unit>
}
