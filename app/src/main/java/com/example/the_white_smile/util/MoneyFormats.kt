package com.example.the_white_smile.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Conversión de importes entre dólares mostrados y centavos almacenados. */
object MoneyFormats {
    fun usd(cents: Long?): String? = cents?.let {
        NumberFormat.getCurrencyInstance(Locale.US).format(it / 100.0)
    }

    fun input(cents: Long?): String = cents?.let {
        BigDecimal.valueOf(it, 2).setScale(2).toPlainString()
    }.orEmpty()

    fun parseUsd(value: String): Long? = try {
        val normalized = value.trim().replace("$", "").replace(",", ".")
        val cents = BigDecimal(normalized)
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        cents.takeIf { it > 0L }
    } catch (_: Exception) {
        null
    }

    fun percentageOf(totalCents: Long, percent: Int): Long =
        (totalCents * percent + 50L) / 100L
}
