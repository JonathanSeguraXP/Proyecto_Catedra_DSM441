package com.example.the_white_smile.core

import com.example.the_white_smile.data.AppointmentRepository
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.FirebaseAuthRepository
import com.example.the_white_smile.data.FirestoreAppointmentRepository
import com.example.the_white_smile.data.LocalTreatmentRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Punto unico de construccion de dependencias (inyeccion manual).
 * Las pantallas piden sus dependencias aqui en lugar de crearlas por su cuenta.
 */
object ServiceLocator {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth) }

    val appointmentRepository: AppointmentRepository by lazy {
        FirestoreAppointmentRepository(firestore, auth)
    }

    val treatmentRepository: TreatmentRepository by lazy { LocalTreatmentRepository }
}
