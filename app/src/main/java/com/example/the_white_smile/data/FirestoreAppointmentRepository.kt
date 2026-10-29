package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Implementacion de [AppointmentRepository] sobre Cloud Firestore.
 * Las citas se guardan en users/{uid}/appointments, de modo que cada
 * usuario solo puede acceder a las suyas.
 */
class FirestoreAppointmentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AppointmentRepository {

    override fun observeAppointments(): Flow<List<Appointment>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = appointmentsRef(uid)
            .orderBy(FIELD_START_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents
                    ?.mapNotNull { it.toAppointment() }
                    .orEmpty()
                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createAppointment(
        service: String,
        startAt: Long,
        dentistId: String,
        dentistName: String
    ): Result<Unit> = runData {
        val data = mapOf(
            FIELD_SERVICE to service,
            FIELD_START_AT to startAt,
            FIELD_STATUS to AppointmentStatus.PENDING.name,
            FIELD_DENTIST_ID to dentistId,
            FIELD_DENTIST_NAME to dentistName,
            FIELD_CREATED_AT to System.currentTimeMillis()
        )
        appointmentsRef(requireUid()).add(data).awaitResult()
        Unit
    }

    override suspend fun cancelAppointment(id: String): Result<Unit> = runData {
        appointmentsRef(requireUid())
            .document(id)
            .update(FIELD_STATUS, AppointmentStatus.CANCELLED.name)
            .awaitResult()
        Unit
    }

    private fun appointmentsRef(uid: String) =
        firestore.collection(USERS).document(uid).collection(APPOINTMENTS)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("No hay una sesion activa")

    private fun DocumentSnapshot.toAppointment(): Appointment? {
        val service = getString(FIELD_SERVICE) ?: return null
        val startAt = getLong(FIELD_START_AT) ?: return null
        return Appointment(
            id = id,
            service = service,
            startAt = startAt,
            status = AppointmentStatus.from(getString(FIELD_STATUS)),
            dentistId = getString(FIELD_DENTIST_ID),
            dentistName = getString(FIELD_DENTIST_NAME)
        )
    }

    /** Ejecuta el bloque y traduce cualquier fallo a un [Result] fallido. */
    private suspend fun <T> runData(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val USERS = "users"
        const val APPOINTMENTS = "appointments"
        const val FIELD_SERVICE = "service"
        const val FIELD_START_AT = "startAt"
        const val FIELD_STATUS = "status"
        const val FIELD_DENTIST_ID = "dentistId"
        const val FIELD_DENTIST_NAME = "dentistName"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
