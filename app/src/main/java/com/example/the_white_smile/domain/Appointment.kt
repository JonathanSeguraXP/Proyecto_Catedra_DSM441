package com.example.the_white_smile.domain

/**
 * Cita agendada por un usuario.
 * [startAt] se guarda en milisegundos desde la epoca (UTC) y se muestra
 * en la zona horaria del dispositivo. [dentistName] se guarda junto con la
 * cita para poder mostrarla aunque el dentista cambie de nombre.
 * [durationMinutes] permite saber cuantos bloques de la agenda ocupa.
 * [patientId] y [patientName] identifican al paciente para que el dentista
 * pueda reconocer la cita en su agenda. Las citas anteriores a esta version
 * no los tienen y solo se pueden consultar.
 */
data class Appointment(
    val id: String,
    val service: String,
    val startAt: Long,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val dentistId: String? = null,
    val dentistName: String? = null,
    val treatmentId: String? = null,
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val patientId: String? = null,
    val patientName: String? = null,
    val totalPriceCents: Long? = null,
    val depositCents: Long? = null,
    val depositPercent: Int? = null,
    val depositStatus: DepositStatus? = null,
    val dentistSuggestion: String? = null
) {
    companion object {
        /** Duracion asumida para citas creadas antes de esta version. */
        const val DEFAULT_DURATION_MINUTES = 30
    }
}

/** Estado del abono; PENDING significa que aún se registra para pago en clínica. */
enum class DepositStatus {
    PENDING,
    PAID;

    companion object {
        fun from(value: String?): DepositStatus? =
            values().firstOrNull { it.name == value }
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
