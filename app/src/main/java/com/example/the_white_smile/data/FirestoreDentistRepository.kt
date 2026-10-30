package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.domain.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Implementacion de [DentistRepository] sobre Cloud Firestore.
 * Cada dentista tiene su perfil en dentists/{uid} y su rol en users/{uid}.
 */
class FirestoreDentistRepository(
    private val firestore: FirebaseFirestore,
    private val accountCreator: DentistAccountCreator
) : DentistRepository {

    override fun observeDentists(): Flow<List<Dentist>> = callbackFlow {
        val registration = dentistsRef()
            .orderBy(FIELD_NAME)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val dentists = snapshot?.documents.orEmpty().mapNotNull { it.toDentist() }
                trySend(dentists)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createDentist(
        displayName: String,
        email: String,
        password: String,
        specialty: String,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Result<Dentist> = runData {
        val account = accountCreator.createAccount(displayName, email, password)
            .getOrElse { throw it }
        val schedule = scheduleFields(workDays, startMinute, endMinute)

        try {
            val batch = firestore.batch()
            batch.set(
                firestore.collection(USERS).document(account.uid),
                mapOf(
                    FIELD_EMAIL to email,
                    FIELD_NAME to displayName,
                    FIELD_ROLE to UserRole.DENTIST.name,
                    FIELD_SPECIALTY to specialty,
                    FIELD_ACTIVE to true
                )
            )
            batch.set(
                dentistsRef().document(account.uid),
                mapOf(
                    FIELD_NAME to displayName,
                    FIELD_EMAIL to email,
                    FIELD_SPECIALTY to specialty,
                    FIELD_ACTIVE to true
                ) + schedule
            )
            batch.commit().awaitResult()

            account.confirm()
            Dentist(
                id = account.uid,
                displayName = displayName,
                email = email,
                specialty = specialty,
                active = true,
                workDays = workDays,
                startMinute = startMinute,
                endMinute = endMinute
            )
        } catch (e: CancellationException) {
            account.discard()
            throw e
        } catch (e: Exception) {
            // Si no se guarda el perfil, se elimina la cuenta para no dejarla a medias.
            account.discard()
            throw e
        }
    }

    override suspend fun setActive(dentistId: String, active: Boolean): Result<Unit> = runData {
        val changes = mapOf(FIELD_ACTIVE to active)
        val batch = firestore.batch()
        batch.set(
            dentistsRef().document(dentistId),
            changes,
            SetOptions.merge()
        )
        batch.set(
            firestore.collection(USERS).document(dentistId),
            changes,
            SetOptions.merge()
        )
        batch.commit().awaitResult()
    }

    override suspend fun updateSchedule(
        dentistId: String,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Result<Unit> = runData {
        dentistsRef()
            .document(dentistId)
            .set(
                scheduleFields(workDays, startMinute, endMinute),
                SetOptions.merge()
            )
            .awaitResult()
        Unit
    }

    private fun dentistsRef() = firestore.collection(DENTISTS)

    /** Los dias se guardan como una lista ordenada para poder leerlos desde la consola. */
    private fun scheduleFields(
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Map<String, Any> = mapOf(
        FIELD_WORK_DAYS to workDays.sorted(),
        FIELD_START_MINUTE to startMinute,
        FIELD_END_MINUTE to endMinute
    )

    private fun DocumentSnapshot.toDentist(): Dentist? {
        if (!exists()) return null
        val email = getString(FIELD_EMAIL)
        val name = getString(FIELD_NAME)?.trim().orEmpty().ifBlank { email.orEmpty() }
        return Dentist(
            id = id,
            displayName = name,
            email = email,
            specialty = getString(FIELD_SPECIALTY).orEmpty(),
            active = getBoolean(FIELD_ACTIVE) ?: true,
            workDays = readWorkDays(),
            startMinute = getLong(FIELD_START_MINUTE)?.toInt()
                ?: Dentist.DEFAULT_START_MINUTE,
            endMinute = getLong(FIELD_END_MINUTE)?.toInt()
                ?: Dentist.DEFAULT_END_MINUTE
        )
    }

    private fun DocumentSnapshot.readWorkDays(): Set<Int> {
        val stored = get(FIELD_WORK_DAYS) as? List<*> ?: return Dentist.DEFAULT_WORK_DAYS
        val days = stored.mapNotNull { (it as? Number)?.toInt() }
            .filter { it in 1..7 }
            .toSet()
        return days.ifEmpty { Dentist.DEFAULT_WORK_DAYS }
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
        const val DENTISTS = "dentists"
        const val USERS = "users"
        const val FIELD_NAME = "displayName"
        const val FIELD_EMAIL = "email"
        const val FIELD_SPECIALTY = "specialty"
        const val FIELD_ROLE = "role"
        const val FIELD_ACTIVE = "active"
        const val FIELD_WORK_DAYS = "workDays"
        const val FIELD_START_MINUTE = "startMinute"
        const val FIELD_END_MINUTE = "endMinute"
    }
}
