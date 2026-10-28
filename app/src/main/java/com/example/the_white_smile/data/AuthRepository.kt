package com.example.the_white_smile.data

import android.app.Activity
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.SessionUser

/**
 * Contrato de autenticacion de la aplicacion.
 * La implementacion concreta puede ser Firebase, un backend propio o un doble de prueba.
 */
interface AuthRepository {

    /** Usuario con sesion iniciada, o null si no hay sesion. */
    val currentUser: SessionUser?

    /** True si existe una sesion activa. */
    fun isLoggedIn(): Boolean

    /** True si el correo de la cuenta ya fue verificado. */
    fun isEmailVerified(): Boolean

    suspend fun signInWithEmail(email: String, password: String): AuthResult<SessionUser>

    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): AuthResult<SessionUser>

    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult<SessionUser>

    suspend fun signInWithGithub(activity: Activity): AuthResult<SessionUser>

    suspend fun sendPasswordReset(email: String): AuthResult<Unit>

    suspend fun updateDisplayName(name: String): AuthResult<SessionUser>

    suspend fun sendEmailVerification(): AuthResult<Unit>

    /** Vuelve a leer el usuario desde el servidor (por ejemplo, tras verificar el correo). */
    suspend fun reloadUser(): AuthResult<Unit>

    fun signOut()
}
