package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.UserProfile
import com.example.the_white_smile.domain.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException

/**
 * Implementacion de [UserRepository] sobre Cloud Firestore.
 * Los perfiles se guardan en users/{uid}; si no existe el documento,
 * el usuario se trata como paciente.
 */
class FirestoreUserRepository(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun getProfile(uid: String): Result<UserProfile?> = runData {
        val snapshot = usersRef().document(uid).get().awaitResult()
        val profile = snapshot.toUserProfile() ?: return@runData null
        if (profile.role != UserRole.DENTIST || !profile.specialty.isNullOrBlank()) {
            return@runData profile
        }

        // Compatibilidad con dentistas creados antes de guardar la especialidad
        // también en users/{uid}.
        val dentist = firestore.collection(DENTISTS).document(uid).get().awaitResult()
        profile.copy(specialty = dentist.getString(FIELD_SPECIALTY))
    }

    private fun usersRef() = firestore.collection(USERS)

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            uid = id,
            email = getString(FIELD_EMAIL),
            displayName = getString(FIELD_NAME),
            role = UserRole.from(getString(FIELD_ROLE)),
            specialty = getString(FIELD_SPECIALTY),
            active = getBoolean(FIELD_ACTIVE) ?: true
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
        const val DENTISTS = "dentists"
        const val FIELD_EMAIL = "email"
        const val FIELD_NAME = "displayName"
        const val FIELD_ROLE = "role"
        const val FIELD_SPECIALTY = "specialty"
        const val FIELD_ACTIVE = "active"
    }
}
