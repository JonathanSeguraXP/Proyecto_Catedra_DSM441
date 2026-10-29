package com.example.the_white_smile.domain

/** Rol del usuario dentro de la aplicacion. */
enum class UserRole {
    PATIENT,
    DENTIST,
    ADMIN;

    companion object {
        /** Convierte el valor guardado en Firestore a un rol conocido. */
        fun from(value: String?): UserRole {
            val clean = value?.trim()?.uppercase() ?: return PATIENT
            return values().firstOrNull { it.name == clean } ?: PATIENT
        }
    }
}
