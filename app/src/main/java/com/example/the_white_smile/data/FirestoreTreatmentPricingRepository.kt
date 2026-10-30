package com.example.the_white_smile.data

import com.example.the_white_smile.core.awaitResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Precios persistentes compartidos por el catálogo y el flujo de citas. */
class FirestoreTreatmentPricingRepository(
    private val firestore: FirebaseFirestore
) : TreatmentPricingRepository {

    override fun observePrices(): Flow<Map<String, Long>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val prices = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.getLong(FIELD_PRICE_CENTS)?.let { document.id to it }
                }.toMap()
                trySend(prices)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setPrice(treatmentId: String, priceCents: Long): Result<Unit> = try {
        firestore.collection(COLLECTION).document(treatmentId)
            .set(mapOf(FIELD_PRICE_CENTS to priceCents))
            .awaitResult()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val COLLECTION = "treatmentPrices"
        const val FIELD_PRICE_CENTS = "priceCents"
    }
}
