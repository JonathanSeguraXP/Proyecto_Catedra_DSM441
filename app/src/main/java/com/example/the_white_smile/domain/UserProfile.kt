package com.example.the_white_smile.domain

/**
 * Perfil del usuario guardado en Firestore.
 * Los pacientes no necesitan un documento propio: si no existe, se
 * considera [UserRole.PATIENT]. El administrador y los dentistas si lo
 * necesitan para acceder a sus pantallas.
 */
data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val role: UserRole = UserRole.PATIENT,
    val active: Boolean = true
)
