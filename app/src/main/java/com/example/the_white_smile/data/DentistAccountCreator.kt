package com.example.the_white_smile.data

/**
 * Crea cuentas en Firebase Authentication sin afectar la sesion activa.
 * La implementacion real utiliza una instancia secundaria de FirebaseApp.
 */
interface DentistAccountCreator {

    /**
     * Crea la cuenta y deja una sesion secundaria abierta para poder
     * confirmarla o descartarla si falla el guardado del perfil.
     */
    suspend fun createAccount(
        displayName: String,
        email: String,
        password: String
    ): Result<PendingDentistAccount>
}

/** Cuenta recien creada, pendiente de confirmar o descartar. */
interface PendingDentistAccount {

    /** UID asignado por Firebase Authentication. */
    val uid: String

    /** Mantiene la cuenta y cierra la sesion secundaria. */
    suspend fun confirm()

    /** Elimina la cuenta y cierra la sesion secundaria. */
    suspend fun discard()
}
