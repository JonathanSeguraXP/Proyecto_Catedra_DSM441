package com.example.the_white_smile.ui.login

import androidx.annotation.StringRes

/** Acciones de una sola vez que la pantalla debe mostrar o ejecutar. */
sealed class LoginEvent {

    /** Mensaje corto ya resuelto a un recurso de texto. */
    data class Message(@param:StringRes val messageRes: Int) : LoginEvent()

    /** Error de Firebase o de red que la pantalla traduce con FirebaseErrors. */
    data class Error(val cause: Throwable?) : LoginEvent()

    /** Credenciales correctas: hay que guardar el correo y navegar al menu. */
    data class LoggedIn(val email: String) : LoginEvent()
}
