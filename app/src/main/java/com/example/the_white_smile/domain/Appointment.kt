package com.example.the_white_smile.domain

/**
 * Cita agendada por un usuario.
 * [startAt] se guarda en milisegundos desde la epoca (UTC) y se muestra
 * en la zona horaria del dispositivo. [dentistName] se guarda junto con la
 * cita para poder mostrarla aunque el dentista cambie de nombre.
 */
data class Appointment(
    val id: String,
    val service: String,
    val startAt: Long,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val dentistId: String? = null,
    val dentistName: String? = null
)

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
