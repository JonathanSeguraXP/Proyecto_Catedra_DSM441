package com.example.the_white_smile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pruebas puras de las reglas de bloques y horarios. */
class AppointmentScheduleTest {

    private val dentist = Dentist(
        id = "d1",
        displayName = "Dra. Ana",
        email = "ana@correo.com",
        specialty = "General",
        workDays = setOf(1, 2, 3, 4, 5),
        startMinute = 8 * 60,
        endMinute = 17 * 60
    )

    /** Lunes 2 de noviembre de 2026 a medianoche. */
    private val monday = dayStart(2026, Calendar.NOVEMBER, 2)

    /** Sabado 7 de noviembre de 2026 a medianoche. */
    private val saturday = dayStart(2026, Calendar.NOVEMBER, 7)

    private fun dayStart(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `una cita de 30 minutos ocupa dos bloques consecutivos`() {
        val start = AppointmentSchedule.atMinute(monday, 10 * 60)

        val indexes = AppointmentSchedule.slotIndexes(start, 30)

        assertEquals(2, indexes.size)
        assertEquals(indexes[0] + 1, indexes[1])
        assertEquals(2, AppointmentSchedule.slotsFor(30))
        assertEquals(3, AppointmentSchedule.slotsFor(31))
    }

    @Test
    fun `las horas se alinean a bloques de 15 minutos`() {
        val start = AppointmentSchedule.atMinute(monday, 10 * 60)

        assertTrue(AppointmentSchedule.isAligned(start))
        assertFalse(AppointmentSchedule.isAligned(start + 7 * 60_000L))
    }

    @Test
    fun `un dia no laboral no tiene horas disponibles`() {
        val times = AppointmentSchedule.availableStarts(
            dentist = dentist,
            dayStartAt = saturday,
            durationMinutes = 30,
            busySlots = emptySet(),
            notBefore = 0
        )

        assertTrue(times.isEmpty())
    }

    @Test
    fun `las horas respetan el rango de atencion`() {
        val times = AppointmentSchedule.availableStarts(
            dentist = dentist,
            dayStartAt = monday,
            durationMinutes = 30,
            busySlots = emptySet(),
            notBefore = 0
        )

        assertEquals(AppointmentSchedule.atMinute(monday, 8 * 60), times.first())
        assertEquals(AppointmentSchedule.atMinute(monday, 16 * 60 + 30), times.last())
        assertTrue(times.size > 1)
    }

    @Test
    fun `un tratamiento largo necesita empezar antes del cierre`() {
        val times = AppointmentSchedule.availableStarts(
            dentist = dentist,
            dayStartAt = monday,
            durationMinutes = 60,
            busySlots = emptySet(),
            notBefore = 0
        )

        assertEquals(AppointmentSchedule.atMinute(monday, 16 * 60), times.last())
        assertFalse(times.contains(AppointmentSchedule.atMinute(monday, 16 * 60 + 15)))
    }

    @Test
    fun `los bloques ocupados se excluyen de la disponibilidad`() {
        val busy = setOf(
            AppointmentSchedule.slotIndex(AppointmentSchedule.atMinute(monday, 9 * 60))
        )

        val times = AppointmentSchedule.availableStarts(
            dentist = dentist,
            dayStartAt = monday,
            durationMinutes = 30,
            busySlots = busy,
            notBefore = 0
        )

        assertFalse(times.contains(AppointmentSchedule.atMinute(monday, 9 * 60)))
        assertFalse(times.contains(AppointmentSchedule.atMinute(monday, 8 * 60 + 45)))
        assertTrue(times.contains(AppointmentSchedule.atMinute(monday, 9 * 60 + 30)))
    }

    @Test
    fun `las horas anteriores al momento actual se descartan`() {
        val notBefore = AppointmentSchedule.atMinute(monday, 10 * 60)

        val times = AppointmentSchedule.availableStarts(
            dentist = dentist,
            dayStartAt = monday,
            durationMinutes = 30,
            busySlots = emptySet(),
            notBefore = notBefore
        )

        assertEquals(notBefore, times.first())
    }

    @Test
    fun `una cita debe terminar dentro del horario`() {
        assertTrue(
            AppointmentSchedule.isWithinSchedule(
                dentist,
                AppointmentSchedule.atMinute(monday, 16 * 60 + 30),
                30
            )
        )
        assertFalse(
            AppointmentSchedule.isWithinSchedule(
                dentist,
                AppointmentSchedule.atMinute(monday, 16 * 60 + 45),
                30
            )
        )
        assertFalse(
            AppointmentSchedule.isWithinSchedule(
                dentist,
                AppointmentSchedule.atMinute(monday, 7 * 60 + 45),
                30
            )
        )
    }
}
