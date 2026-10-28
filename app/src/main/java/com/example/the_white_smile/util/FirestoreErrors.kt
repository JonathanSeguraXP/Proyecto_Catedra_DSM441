package com.example.the_white_smile.util

import android.content.Context
import com.example.the_white_smile.R
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Traduce los codigos de error de Cloud Firestore a mensajes
 * claros en espanol para mostrar al usuario.
 */
object FirestoreErrors {

    fun message(context: Context, throwable: Throwable?): String {
        val code = (throwable as? FirebaseFirestoreException)?.code
        val resId = when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_firestore_permission
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_connection
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> R.string.error_connection_timeout
            FirebaseFirestoreException.Code.NOT_FOUND -> R.string.error_firestore_not_found
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                R.string.error_firestore_unavailable
            else -> R.string.error_unknown
        }
        return context.getString(resId)
    }
}
