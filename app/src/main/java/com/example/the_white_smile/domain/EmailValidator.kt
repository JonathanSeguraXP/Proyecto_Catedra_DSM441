package com.example.the_white_smile.domain

/** Valida el formato de un correo electronico. */
interface EmailValidator {

    fun isValid(email: String): Boolean
}
