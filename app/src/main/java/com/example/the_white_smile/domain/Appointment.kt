package com.example.the_white_smile.domain

/**
 * Cita agendada por un usuario.
 * [startAt] se guarda en milisegundos desde la epoca (UTC) y se muestra
 * en la zona horaria del dispositivo. [dentistName] se guarda junto con la
 * cita para poder mostrarla aunque el dentista cambie de nombre.
 * [durationMinutes] permite saber cuantos bloques de la agenda ocupa.
 */
data class Appointment(
    val id: String,
    val service: String,
    val startAt: Long,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val dentistId: String? = null,
    val dentistName: String? = null,
    val treatmentId: String? = null,
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES
) {
    companion object {
        /** Duracion asumida para citas creadas antes de esta version. */
        const val DEFAULT_DURATION_MINUTES = 30
    }
}

/** Estados posibles de una cita. */
enum class AppointmentStatus {
    PENDING,
    CANCELLED,
    COMPLETED;

    companion object {
        /** Convierte el valor guardado en Firestore a un estado conocido. */
        fun from(value: String?): AppointmentStatus =
            values().firstOrNull { it.name == value } ?: PENDING
    }
}
