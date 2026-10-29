package com.example.the_white_smile.ui.dentists

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla de dentistas debe mostrar. */
sealed class DentistsEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : DentistsEvent()

    /** Cuenta creada correctamente; incluye el nombre del dentista. */
    data class Created(val displayName: String) : DentistsEvent()

    /**
     * Error de Firebase Authentication o de Cloud Firestore.
     * La pantalla decide con que traductor mostrarlo.
     */
    data class Error(val cause: Throwable?) : DentistsEvent()
}
