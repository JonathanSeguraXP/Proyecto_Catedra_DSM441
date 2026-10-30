package com.example.the_white_smile.data

import com.example.the_white_smile.domain.Treatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Contrato de acceso al catalogo de tratamientos.
 * La implementacion actual es local; otra podria leerlo de Firestore
 * sin cambiar los ViewModel ni las pantallas.
 */
interface TreatmentRepository {

    fun getTreatments(): List<Treatment>

    fun getTreatment(id: String): Treatment?

    fun observeCatalog(): Flow<List<Treatment>> = flowOf(getTreatments())

    suspend fun saveTreatment(treatment: Treatment): Result<Unit> =
        Result.failure(UnsupportedOperationException("Edición no disponible"))

    suspend fun deleteTreatment(treatmentId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Eliminación no disponible"))
}
