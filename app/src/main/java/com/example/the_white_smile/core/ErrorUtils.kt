package com.example.the_white_smile.core

/**
 * El usuario cerro el selector de cuentas o el flujo externo:
 * no es un error que debamos mostrarle.
 */
fun Throwable?.isUserCancelled(): Boolean {
    val text = this?.message ?: return false
    return text.contains("cancelled", ignoreCase = true) ||
        text.contains("canceled", ignoreCase = true)
}
