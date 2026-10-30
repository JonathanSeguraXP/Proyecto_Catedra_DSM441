package com.example.the_white_smile.data

import com.example.the_white_smile.domain.UserProfile

/** Contrato de acceso a los perfiles y roles de los usuarios. */
interface UserRepository {

    /**
     * Perfil del usuario, o null si es un paciente sin documento propio.
     * Los pacientes se reconocen por defecto y no requieren registro extra.
     */
    suspend fun getProfile(uid: String): Result<UserProfile?>

    /** Guarda la miniatura del perfil propio; la implementacion puede crear el perfil paciente. */
    suspend fun updateProfilePhoto(uid: String, photoBase64: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("No se puede actualizar la foto"))
}
