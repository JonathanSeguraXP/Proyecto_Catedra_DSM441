package com.example.the_white_smile.data

import android.util.Patterns
import com.example.the_white_smile.domain.EmailValidator

/** Implementacion de [EmailValidator] con el patron que provee Android. */
class AndroidEmailValidator : EmailValidator {

    override fun isValid(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
