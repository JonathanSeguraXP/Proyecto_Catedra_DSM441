package com.example.the_white_smile.domain

/**
 * Resultado de una operacion de autenticacion.
 * Evita que las pantallas conozcan las excepciones de Firebase.
 */
sealed class AuthResult<out T> {

    data class Success<T>(val value: T) : AuthResult<T>()

    data class Error(val cause: Throwable?) : AuthResult<Nothing>()
}
