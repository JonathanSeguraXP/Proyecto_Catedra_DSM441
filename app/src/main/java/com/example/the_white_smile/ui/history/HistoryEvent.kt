package com.example.the_white_smile.ui.history

/** Acciones de una sola vez que la pantalla de historial debe mostrar. */
sealed class HistoryEvent {

    /** Error de Firestore o de red que la pantalla traduce a un mensaje. */
    data class Error(val cause: Throwable?) : HistoryEvent()
}
