package com.example.the_white_smile.ui.menu

import com.example.the_white_smile.domain.UserRole

/** Estado de la pantalla del menu principal. */
data class MenuUiState(
    /** Nombre para saludar; null cuando el usuario no tiene nombre guardado. */
    val displayName: String? = null,
    /** Miniatura del perfil cuando la cuenta pertenece a un dentista. */
    val profilePhotoBase64: String? = null,
    /** Rol del usuario; null mientras se consulta el perfil. */
    val role: UserRole? = null
)
