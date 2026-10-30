package com.example.the_white_smile.domain

/**
 * Dentista del consultorio.
 * Se guarda en dentists/{id}; el id coincide con el UID de su cuenta de
 * Firebase Authentication.
 *
 * [workDays] usa el dia ISO: 1 = lunes y 7 = domingo.
 * [startMinute] y [endMinute] son minutos desde la medianoche.
 */
data class Dentist(
    val id: String,
    val displayName: String,
    val email: String?,
    val specialty: String,
    val profilePhotoBase64: String? = null,
    val active: Boolean = true,
    val workDays: Set<Int> = DEFAULT_WORK_DAYS,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE
) {
    companion object {
        /** Lunes a viernes. */
        val DEFAULT_WORK_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5)

        /** 08:00. */
        const val DEFAULT_START_MINUTE = 8 * 60

        /** 17:00. */
        const val DEFAULT_END_MINUTE = 17 * 60
    }
}
