package com.example.the_white_smile.core

import android.content.Context
import com.example.the_white_smile.data.AppointmentRepository
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.DentistRepository
import com.example.the_white_smile.data.FirebaseAuthRepository
import com.example.the_white_smile.data.FirebaseDentistAccountCreator
import com.example.the_white_smile.data.FirestoreAppointmentRepository
import com.example.the_white_smile.data.FirestoreDentistRepository
import com.example.the_white_smile.data.FirestoreUserRepository
import com.example.the_white_smile.data.FirestoreTreatmentRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.TreatmentPricingRepository
import com.example.the_white_smile.data.FirestoreTreatmentPricingRepository
import com.example.the_white_smile.data.UserRepository
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

    val treatmentRepository: TreatmentRepository by lazy {
        FirestoreTreatmentRepository(firestore)
    }

    val treatmentPricingRepository: TreatmentPricingRepository by lazy {
        FirestoreTreatmentPricingRepository(firestore)
    }

    val userRepository: UserRepository by lazy { FirestoreUserRepository(firestore) }

    /**
     * Repositorio de dentistas. Recibe el contexto porque la creacion de
     * cuentas necesita la instancia secundaria de FirebaseApp.
     */
    fun dentistRepository(context: Context): DentistRepository =
        FirestoreDentistRepository(
            firestore = firestore,
            accountCreator = FirebaseDentistAccountCreator(context.applicationContext)
        )
}
