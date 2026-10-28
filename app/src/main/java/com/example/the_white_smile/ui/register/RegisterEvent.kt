package com.example.the_white_smile.ui.register

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla debe mostrar o ejecutar. */
sealed class RegisterEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : RegisterEvent()

    /** Error de Firebase que la pantalla traduce con FirebaseErrors. */
    data class Error(val cause: Throwable?) : RegisterEvent()

    /** Cuenta creada: la pantalla avisa y vuelve al login. */
    data object Registered : RegisterEvent()
}
