package com.example.the_white_smile.domain

import java.util.Calendar

/**
 * Reglas de horarios compartidas por las citas.
 *
 * Toda la agenda se divide en bloques de [SLOT_MINUTES] minutos. Un bloque
 * se identifica por su indice, calculado desde la epoca, de modo que dos
 * dispositivos en la misma zona horaria obtienen el mismo identificador.
 */
object AppointmentSchedule {

    /** Duracion de cada bloque reservable. */
    const val SLOT_MINUTES = 15

    /** Duracion de un bloque en milisegundos. */
    const val SLOT_MILLIS = SLOT_MINUTES * 60_000L

    /** Cantidad de bloques que ocupa un tratamiento. */
    fun slotsFor(durationMinutes: Int): Int =
        (durationMinutes.coerceAtLeast(SLOT_MINUTES) + SLOT_MINUTES - 1) / SLOT_MINUTES

    /** Indice del bloque que contiene [millis]. */
    fun slotIndex(millis: Long): Long = Math.floorDiv(millis, SLOT_MILLIS)

    /** Inicio del bloque con el indice indicado. */
    fun slotStart(slotIndex: Long): Long = slotIndex * SLOT_MILLIS

    /** Todos los bloques que ocupa una cita desde [startAt]. */
    fun slotIndexes(startAt: Long, durationMinutes: Int): List<Long> {
        val first = slotIndex(startAt)
        return (0 until slotsFor(durationMinutes)).map { first + it }
    }

    /** La cita empieza en un bloque exacto de [SLOT_MINUTES]. */
    fun isAligned(millis: Long): Boolean = slotStart(slotIndex(millis)) == millis

    /** Medianoche del dia que contiene [millis], en la zona del dispositivo. */
    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Medianoche del dia siguiente al que contiene [millis]. */
    fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(millis)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    /** Dia ISO de la semana: 1 = lunes, 7 = domingo. */
    fun dayOfWeek(millis: Long): Int = when (
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
    ) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }

    /** Minutos transcurridos desde la medianoche. */
    fun minuteOfDay(millis: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /** Convierte un minuto del dia en una marca de tiempo del dia indicado. */
    fun atMinute(dayStartAt: Long, minuteOfDay: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayStartAt
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Indica si el dentista atiende ese dia en ese rango. */
    fun isWithinSchedule(
        dentist: Dentist,
        startAt: Long,
        durationMinutes: Int
    ): Boolean {
        if (!isAligned(startAt)) return false
        if (dayOfWeek(startAt) !in dentist.workDays) return false
        val startMinute = minuteOfDay(startAt)
        return startMinute >= dentist.startMinute &&
            startMinute + durationMinutes <= dentist.endMinute
    }

    /**
     * Inicios posibles para un tratamiento en un dia concreto.
     * [busySlots] contiene los indices de los bloques ya ocupados.
     * [notBefore] descarta las horas que ya pasaron.
     */
    fun availableStarts(
        dentist: Dentist,
        dayStartAt: Long,
        durationMinutes: Int,
        busySlots: Set<Long>,
        notBefore: Long
    ): List<Long> {
        if (dayOfWeek(dayStartAt) !in dentist.workDays) return emptyList()

        val duration = durationMinutes.coerceAtLeast(SLOT_MINUTES)
        val lastStart = dentist.endMinute - duration
        if (lastStart < dentist.startMinute) return emptyList()

        return generateSequence(dentist.startMinute) { it + SLOT_MINUTES }
            .takeWhile { it <= lastStart }
            .map { atMinute(dayStartAt, it) }
            .filter { it >= notBefore }
            .filter { start ->
                slotIndexes(start, duration).none { it in busySlots }
            }
            .toList()
    }
}
