package com.example.the_white_smile.util

import android.content.Context
import com.example.the_white_smile.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formatos de fecha y hora usados en las pantallas de citas. */
object DateFormats {

    fun dateTime(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

    fun date(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

    fun time(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    /** Formatea un minuto del dia como HH:mm. */
    fun minuteOfDay(minuteOfDay: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

    /** Resumen corto de los dias de atencion: Lun, Mar, ... */
    fun shortDays(context: Context, days: Set<Int>): String =
        days.sorted().mapNotNull { day ->
            val res = when (day) {
                1 -> R.string.day_mon
                2 -> R.string.day_tue
                3 -> R.string.day_wed
                4 -> R.string.day_thu
                5 -> R.string.day_fri
                6 -> R.string.day_sat
                7 -> R.string.day_sun
                else -> null
            }
            res?.let(context::getString)
        }.joinToString(", ")
}
