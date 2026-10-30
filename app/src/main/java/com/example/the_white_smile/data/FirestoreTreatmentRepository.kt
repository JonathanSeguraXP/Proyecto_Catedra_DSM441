package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.Treatment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Catalogo predeterminado con cambios editables sincronizados en Firestore. */
class FirestoreTreatmentRepository(
    private val firestore: FirebaseFirestore,
    private val defaults: TreatmentRepository = LocalTreatmentRepository
) : TreatmentRepository {

    override fun getTreatments(): List<Treatment> = defaults.getTreatments()

    override fun getTreatment(id: String): Treatment? = defaults.getTreatment(id)

    override fun observeCatalog(): Flow<List<Treatment>> = callbackFlow {
        val registration = firestore.collection(COLLECTION).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val overrides = snapshot?.documents.orEmpty().mapNotNull { it.toTreatment() }
                .associateBy { it.id }
            trySend(defaults.getTreatments().map { overrides[it.id] ?: it })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveTreatment(treatment: Treatment): Result<Unit> = try {
        val data = mapOf(
            "name" to treatment.name,
            "summary" to treatment.summary,
            "description" to treatment.description,
            "durationMinutes" to treatment.durationMinutes,
            "recommendations" to treatment.recommendations,
            "priceCents" to treatment.priceCents
        )
        firestore.collection(COLLECTION).document(treatment.id).set(data).awaitResult()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun DocumentSnapshot.toTreatment(): Treatment? {
        val base = defaults.getTreatment(id) ?: return null
        return base.copy(
            name = getString("name") ?: base.name,
            summary = getString("summary") ?: base.summary,
            description = getString("description") ?: base.description,
            durationMinutes = getLong("durationMinutes")?.toInt() ?: base.durationMinutes,
            recommendations = get("recommendations") as? List<String> ?: base.recommendations,
            priceCents = getLong("priceCents") ?: base.priceCents
        )
    }

    private companion object {
        const val COLLECTION = "treatments"
    }
}
