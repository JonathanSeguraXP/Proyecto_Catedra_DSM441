package com.example.the_white_smile.ui.appointments

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla de citas debe mostrar. */
sealed class AppointmentsEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : AppointmentsEvent()

    /** Error de Firestore o de red que la pantalla traduce a un mensaje. */
    data class Error(val cause: Throwable?) : AppointmentsEvent()
}
