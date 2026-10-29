package com.example.the_white_smile.domain

/**
 * Tratamiento odontologico del catalogo.
 * [recommendations] son indicaciones breves que se muestran en el detalle.
 */
data class Treatment(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val durationMinutes: Int,
    val recommendations: List<String>
)
