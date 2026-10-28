package com.example.the_white_smile.util

import android.content.Context
import com.example.the_white_smile.R
import com.google.firebase.auth.FirebaseAuthException

/**
 * Traduce los códigos de error de Firebase Authentication a mensajes
 * claros en español para mostrar al usuario.
 */
object FirebaseErrors {

    fun message(context: Context, throwable: Throwable?): String {
        val code = (throwable as? FirebaseAuthException)?.errorCode
        val resId = when (code) {
            "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email
            "ERROR_WRONG_PASSWORD" -> R.string.error_wrong_password
            "ERROR_USER_NOT_FOUND" -> R.string.error_user_not_found
            "ERROR_USER_DISABLED" -> R.string.error_user_disabled
            "ERROR_TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
            "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.error_email_already_in_use
            "ERROR_WEAK_PASSWORD" -> R.string.error_weak_password
            "ERROR_OPERATION_NOT_ALLOWED" -> R.string.error_operation_not_allowed
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> R.string.error_credential_already_in_use
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                R.string.error_account_exists_different_credential
            "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_no_connection
            else -> R.string.error_unknown
        }
        return context.getString(resId)
    }
}
