package com.example.the_white_smile.ui.agenda

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla de agenda debe mostrar. */
sealed class DentistAgendaEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : DentistAgendaEvent()

    /** Error de Firestore o de red que la pantalla traduce a un mensaje. */
    data class Error(val cause: Throwable?) : DentistAgendaEvent()
}
