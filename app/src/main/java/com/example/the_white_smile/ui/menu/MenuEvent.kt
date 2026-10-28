package com.example.the_white_smile.ui.menu

/** Acciones de una sola vez que la pantalla debe ejecutar. */
sealed class MenuEvent {

    /** La sesion se cerro: hay que volver al login. */
    data object LoggedOut : MenuEvent()
}
