package com.example.the_white_smile.data

import kotlinx.coroutines.flow.Flow

/** Lee y guarda los precios que administra el consultorio, expresados en centavos. */
interface TreatmentPricingRepository {
    fun observePrices(): Flow<Map<String, Long>>

    suspend fun setPrice(treatmentId: String, priceCents: Long): Result<Unit>
}
