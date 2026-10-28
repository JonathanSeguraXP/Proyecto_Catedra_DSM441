package com.example.the_white_smile.util

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
}
