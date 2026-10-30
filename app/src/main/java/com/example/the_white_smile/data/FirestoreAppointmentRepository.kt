package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AppointmentSchedule
import com.example.the_white_smile.domain.AppointmentStatus
import com.example.the_white_smile.domain.SlotConflictException
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
 * Las citas se guardan en users/{uid}/appointments y los bloques ocupados en
 * dentists/{dentistId}/slots, de modo que todos los pacientes ven el mismo
 * horario sin poder leer las citas de los demas.
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

    override fun observeBusySlots(
        dentistId: String,
        dayStartAt: Long,
        dayEndAt: Long
    ): Flow<Set<Long>> = callbackFlow {
        val registration = slotsRef(dentistId)
            .whereGreaterThanOrEqualTo(FIELD_START_AT, dayStartAt)
            .whereLessThan(FIELD_START_AT, dayEndAt)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val busy = snapshot?.documents
                    ?.mapNotNull { it.getLong(FIELD_START_AT) }
                    ?.map { AppointmentSchedule.slotIndex(it) }
                    ?.toSet()
                    .orEmpty()
                trySend(busy)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createAppointment(
        service: String,
        startAt: Long,
        treatmentId: String,
        durationMinutes: Int,
        dentistId: String,
        dentistName: String
    ): Result<Unit> = runData {
        val uid = requireUid()
        val appointmentRef = appointmentsRef(uid).document()
        val indexList = AppointmentSchedule.slotIndexes(startAt, durationMinutes)
        val slotRefs = indexList.map { slotsRef(dentistId).document(it.toString()) }
        val createdAt = System.currentTimeMillis()

        firestore.runTransaction { transaction ->
            // Todas las lecturas van antes de cualquier escritura.
            val occupied = slotRefs.map { transaction.get(it) }.any { it.exists() }
            if (occupied) throw SlotConflictException()

            slotRefs.forEachIndexed { position, ref ->
                transaction.set(
                    ref,
                    mapOf(
                        FIELD_START_AT to AppointmentSchedule.slotStart(indexList[position]),
                        FIELD_APPOINTMENT_ID to appointmentRef.id,
                        FIELD_PATIENT_ID to uid,
                        FIELD_CREATED_AT to createdAt
                    )
                )
            }
            transaction.set(
                appointmentRef,
                mapOf(
                    FIELD_SERVICE to service,
                    FIELD_START_AT to startAt,
                    FIELD_STATUS to AppointmentStatus.PENDING.name,
                    FIELD_TREATMENT_ID to treatmentId,
                    FIELD_DURATION_MINUTES to durationMinutes,
                    FIELD_DENTIST_ID to dentistId,
                    FIELD_DENTIST_NAME to dentistName,
                    FIELD_CREATED_AT to createdAt
                )
            )
        }.awaitResult()
        Unit
    }

    override suspend fun cancelAppointment(
        appointment: Appointment
    ): Result<Unit> = runData {
        val uid = requireUid()
        val appointmentRef = appointmentsRef(uid).document(appointment.id)
        val dentistId = appointment.dentistId

        if (dentistId == null) {
            appointmentRef
                .update(FIELD_STATUS, AppointmentStatus.CANCELLED.name)
                .awaitResult()
            return@runData
        }

        val indexList = AppointmentSchedule.slotIndexes(
            appointment.startAt,
            appointment.durationMinutes
        )
        val slotRefs = indexList.map { slotsRef(dentistId).document(it.toString()) }

        firestore.runTransaction { transaction ->
            val snapshots = slotRefs.map { transaction.get(it) }
            snapshots.forEachIndexed { position, snapshot ->
                val owner = snapshot.getString(FIELD_APPOINTMENT_ID)
                if (snapshot.exists() && owner == appointment.id) {
                    transaction.delete(slotRefs[position])
                }
            }
            transaction.update(
                appointmentRef,
                FIELD_STATUS,
                AppointmentStatus.CANCELLED.name
            )
        }.awaitResult()
        Unit
    }

    private fun appointmentsRef(uid: String) =
        firestore.collection(USERS).document(uid).collection(APPOINTMENTS)

    private fun slotsRef(dentistId: String) =
        firestore.collection(DENTISTS).document(dentistId).collection(SLOTS)

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
            dentistName = getString(FIELD_DENTIST_NAME),
            treatmentId = getString(FIELD_TREATMENT_ID),
            durationMinutes = getLong(FIELD_DURATION_MINUTES)?.toInt()
                ?: Appointment.DEFAULT_DURATION_MINUTES
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
        const val DENTISTS = "dentists"
        const val SLOTS = "slots"
        const val FIELD_SERVICE = "service"
        const val FIELD_START_AT = "startAt"
        const val FIELD_STATUS = "status"
        const val FIELD_TREATMENT_ID = "treatmentId"
        const val FIELD_DURATION_MINUTES = "durationMinutes"
        const val FIELD_DENTIST_ID = "dentistId"
        const val FIELD_DENTIST_NAME = "dentistName"
        const val FIELD_APPOINTMENT_ID = "appointmentId"
        const val FIELD_PATIENT_ID = "patientId"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
