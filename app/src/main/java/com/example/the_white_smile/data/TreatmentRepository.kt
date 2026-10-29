package com.example.the_white_smile.data

import com.example.the_white_smile.domain.Treatment

/**
 * Contrato de acceso al catalogo de tratamientos.
 * La implementacion actual es local; otra podria leerlo de Firestore
 * sin cambiar los ViewModel ni las pantallas.
 */
interface TreatmentRepository {

    fun getTreatments(): List<Treatment>

    fun getTreatment(id: String): Treatment?
}
