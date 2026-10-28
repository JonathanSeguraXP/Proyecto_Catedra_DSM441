package com.example.the_white_smile.ui.profile

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla de perfil debe mostrar. */
sealed class ProfileEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : ProfileEvent()

    /** Error de Firebase o de red que la pantalla traduce con FirebaseErrors. */
    data class Error(val cause: Throwable?) : ProfileEvent()
}
