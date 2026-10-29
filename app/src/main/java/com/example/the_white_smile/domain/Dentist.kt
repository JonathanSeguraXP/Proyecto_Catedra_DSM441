package com.example.the_white_smile.domain

/**
 * Dentista del consultorio.
 * Se guarda en dentists/{id}; el id coincide con el UID de su cuenta de
 * Firebase Authentication.
 */
data class Dentist(
    val id: String,
    val displayName: String,
    val email: String?,
    val specialty: String,
    val active: Boolean = true
)
