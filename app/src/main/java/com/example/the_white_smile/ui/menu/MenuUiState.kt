package com.example.the_white_smile.ui.menu

/** Estado de la pantalla del menu principal. */
data class MenuUiState(
    /** Nombre para saludar; null cuando el usuario no tiene nombre guardado. */
    val displayName: String? = null
)
