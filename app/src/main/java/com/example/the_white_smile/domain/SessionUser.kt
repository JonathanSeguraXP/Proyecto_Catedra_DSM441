package com.example.the_white_smile.domain

/** Usuario autenticado, desacoplado de Firebase. */
data class SessionUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
